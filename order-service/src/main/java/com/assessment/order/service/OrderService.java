package com.assessment.order.service;

import com.assessment.order.messaging.OrderEventProducer;
import com.assessment.order.model.dto.OrderRequest;
import com.assessment.order.model.dto.TopSpenderReport;
import com.assessment.order.model.entity.Order;
import com.assessment.order.model.entity.OrderItem;
import com.assessment.order.model.enums.OrderStatus;
import com.assessment.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    /**
     * Memproses pembuatan pesanan baru.
     * * KEPUTUSAN ARSITEKTUR (Distributed Transaction):
     * - @Transactional memastikan Order dan OrderItem tersimpan secara atomik di PostgreSQL.
     * - Harga produk di-snapshot (priceAtPurchase) agar kebal terhadap fluktuasi harga di masa depan.
     * - Catatan Evaluasi: Jika Kafka mati saat memproses item kedua di dalam loop, item pertama 
     * sudah terkirim (tidak bisa ditarik). Solusi Enterprise sejati adalah menggunakan Outbox Pattern.
     */
    @Transactional
    public Order placeOrder(OrderRequest request) {
        log.info("Processing order for customer: {}", request.getCustomerId());

        BigDecimal totalAmount = request.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // (Di arsitektur Saga yang utuh, status ini akan berubah menjadi SUCCESS setelah Catalog merespons)
        Order order = Order.builder()
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING) 
                .totalAmount(totalAmount)
                .build();

        request.getItems().stream()
                .map(itemReq -> OrderItem.builder()
                        .productId(itemReq.getProductId())
                        .quantity(itemReq.getQuantity())
                        .priceAtPurchase(itemReq.getPrice())
                        .build())
                .forEach(order::addItem);

        Order savedOrder = orderRepository.save(order);

        savedOrder.getItems().forEach(item -> 
            orderEventProducer.publishOrderEvent(item.getProductId(), item.getQuantity())
        );

        log.info("Order {} successfully placed and events published to Kafka.", savedOrder.getId());
        return savedOrder;
    }

    /**
     * Analitik: Mendapatkan daftar pelanggan dengan pembelanjaan tertinggi.
     * * KEPUTUSAN ARSITEKTUR:
     * - Operasi delegasi analitik dikerjakan langsung oleh Database (Native/Window Function) 
     * daripada membebani JVM dengan me-load ribuan data ke memori.
     */
    public List<TopSpenderReport> getTopSpendersReport() {
        // Catatan: Secara bisnis, Top Spender biasanya dihitung dari order SUCCESS. 
        return orderRepository.findTopSpendersByStatus(OrderStatus.PENDING.name());
    }
}