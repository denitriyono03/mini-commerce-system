package com.assessment.catalog.service;

import com.assessment.catalog.model.document.ProductDocument;
import com.assessment.catalog.model.dto.ProductDTO;
import com.assessment.catalog.model.entity.Product;
import com.assessment.catalog.repository.ProductRepository;
import com.assessment.catalog.repository.ProductSearchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;

    /**
     * Menyimpan produk ke Database Utama (PostgreSQL) dan menyinkronkannya ke Elasticsearch.
     * * KEPUTUSAN ARSITEKTUR (High Availability & Eventual Consistency):
     * - Menggunakan try-catch pada blok Elasticsearch. Jika Elastic down, transaksi DB utama tetap Commit.
     * - PostgreSQL bertindak sebagai Source of Truth (SoT). Pencarian mungkin tidak akurat sementara waktu, 
     * tetapi fungsionalitas admin (input data) tidak akan terblokir oleh kegagalan sistem sekunder.
     */
    @Transactional
    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        
        try {
            ProductDocument doc = ProductDocument.builder()
                    .id(savedProduct.getId().toString())
                    .name(savedProduct.getName())
                    .description(savedProduct.getDescription())
                    .price(savedProduct.getPrice())
                    .stock(savedProduct.getStock())
                    .build();
            searchRepository.save(doc);
        } catch (Exception e) {
            log.error("Sync to Elasticsearch failed for product ID {}: {}", savedProduct.getId(), e.getMessage());
        }
        
        return savedProduct;
    }

    /**
     * Mengambil detail produk berdasarkan ID.
     * * KEPUTUSAN ARSITEKTUR (Cache-Aside Pattern):
     * - Operasi baca/detail produk adalah operasi dengan traffic tertinggi di e-commerce.
     * - Redis digunakan sebagai layer pertama untuk menekan beban koneksi ke PostgreSQL.
     * - Jika terjadi Cache Miss, data diambil dari DB lalu otomatis dimasukkan ke Redis oleh Spring Cache.
     */
    @Cacheable(value = "productDetail", key = "#id")
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    /**
     * Pencarian produk secara Full-Text.
     * * KEPUTUSAN ARSITEKTUR (Decoupling & DTO Projection):
     * - PostgreSQL di-bypass total untuk operasi pencarian demi performa (Elasticsearch lebih superior untuk teks).
     * - Menggunakan DTO untuk memisahkan struktur storage (Document) dengan API Contract (JSON Response).
     * - Menggunakan toPlainString() untuk memastikan frontend menerima angka absolut, menghindari cacat format E-notation (Scientific).
     */
    public List<ProductDTO> searchProducts(String keyword) {
        List<ProductDocument> docs = searchRepository.findByNameOrDescription(keyword, keyword);

        return docs.stream()
            .map(doc -> ProductDTO.builder()
                .id(doc.getId())
                .name(doc.getName())
                .description(doc.getDescription())
                .price(doc.getPrice())
                .stock(doc.getStock())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Mengurangi stok produk. Dipanggil secara asinkron oleh Kafka Listener.
     * * KEPUTUSAN ARSITEKTUR (Data Consistency & Cache Invalidation):
     * - Bypass @Cacheable: Secara eksplisit memanggil productRepository.findById() untuk memastikan 
     * kalkulasi stok dilakukan menggunakan data mentah terbaru dari DB, bukan data basi dari Redis.
     * - @CacheEvict: Setelah stok di-update di DB, memori Redis dihapus secara hard-delete agar 
     * pemanggilan getProductById() selanjutnya dipaksa membaca stok terbaru dari DB.
     * - Sinkronisasi balik ke Elastic: Menjaga konsistensi data pencarian agar pelanggan tidak melihat "Stok Tersedia" di hasil pencarian.
     */
    @Transactional
    @CacheEvict(value = "productDetail", key = "#productId")
    public void reduceStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        if (product.getStock() < quantity) {
            throw new RuntimeException("Insufficient stock for product id: " + productId);
        }

        product.setStock(product.getStock() - quantity);
        Product updatedProduct = productRepository.save(product);

        try {
            ProductDocument doc = ProductDocument.builder()
                    .id(updatedProduct.getId().toString())
                    .name(updatedProduct.getName())
                    .description(updatedProduct.getDescription())
                    .price(updatedProduct.getPrice())
                    .stock(updatedProduct.getStock())
                    .build();
            searchRepository.save(doc);
        } catch (Exception e) {
            log.error("Failed to update stock in Elasticsearch for product ID {}: {}", productId, e.getMessage());
        }
    }
}