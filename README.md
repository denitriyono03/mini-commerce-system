# 🛒 E-Commerce Microservices Architecture

Sistem backend e-commerce berbasis **Microservices Architecture** yang dirancang untuk performa tinggi, skalabilitas, dan konsistensi data menggunakan pendekatan **Event-Driven Architecture**.

---

## 🚀 Fitur Utama

### 🔍 High-Performance Search

Pencarian produk full-text menggunakan **Elasticsearch** untuk memberikan hasil pencarian yang cepat dan relevan.

### ⚡ In-Memory Cache

Akses detail produk dengan latensi rendah menggunakan **Redis Cache**.

### 📦 Event-Driven Transactions

Pengurangan stok dilakukan secara asinkron menggunakan **Apache Kafka** untuk menjaga loose coupling antar service.

### 📊 Data Analytics

Analitik data real-time menggunakan **SQL Window Function** pada PostgreSQL.

### 🐳 Resilient Infrastructure

Seluruh komponen dijalankan menggunakan **Docker Compose** sehingga mudah untuk deployment dan pengembangan lokal.

---

# 🏗️ Arsitektur Sistem

## Catalog Service

Bertanggung jawab untuk:

* Mengelola data produk
* Sinkronisasi data ke Elasticsearch
* Manajemen cache menggunakan Redis
* Mengurangi stok berdasarkan event dari Kafka

## Order Service

Bertanggung jawab untuk:

* Mengelola transaksi pesanan
* Menyimpan data order
* Mempublikasikan event ke Kafka untuk pembaruan stok

---

# 🛠️ Prerequisites

Pastikan environment Anda telah memiliki:

* Docker
* Docker Compose v2+
* JDK 21
* Maven atau Gradle

---

# 🏃 Menjalankan Aplikasi

## 1. Clone Repository

```bash
git clone https://github.com/denitriyono03/mini-commerce-system
cd mini-commerce-system
```

## 2. Build Aplikasi

Masuk ke folder masing-masing service lalu jalankan:

```bash
./mvnw clean package -DskipTests
```

Atau menggunakan Maven:

```bash
mvn clean package -DskipTests
```

## 3. Jalankan Seluruh Infrastruktur

```bash
docker compose up --build -d
```

## 4. Verifikasi Service

```bash
docker compose ps
```

Melihat log:

```bash
docker compose logs -f
```

---

# 📋 API Specifications

## 1. Catalog Service

**Base URL**

```text
http://localhost:8081
```

### Create Product

| Method | Endpoint        | Description             |
| ------ | --------------- | ----------------------- |
| POST   | `/api/products` | Menambahkan produk baru |

#### Example

```bash
curl -X POST http://localhost:8081/api/products \
-H "Content-Type: application/json" \
-d '{
    "name": "MacBook Pro M5",
    "description": "Laptop super cepat untuk coding",
    "price": 25000000,
    "stock": 10
}'
```

### Get Product Detail

| Method | Endpoint             | Description                                       |
| ------ | -------------------- | ------------------------------------------------- |
| GET    | `/api/products/{id}` | Mengambil detail produk (menggunakan Redis Cache) |

#### Example

```bash
curl -X GET \
"http://localhost:8081/api/products/1"
```

### Search Product

| Method | Endpoint               | Description                            |
| ------ | ---------------------- | -------------------------------------- |
| GET    | `/api/products/search` | Pencarian produk melalui Elasticsearch |

#### Example

```bash
curl -X GET \
"http://localhost:8081/api/products/search?q=coding"
```

---

## 2. Order Service

**Base URL**

```text
http://localhost:8082
```

### Create Order

| Method | Endpoint      | Description          |
| ------ | ------------- | -------------------- |
| POST   | `/api/orders` | Membuat pesanan baru |

#### Example

```bash
curl -X POST http://localhost:8082/api/orders \
-H "Content-Type: application/json" \
-d '{
  "customerId": "CUST-001",
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "price": 20000000
    }
  ]
}'
```

### Top Spenders Report

| Method | Endpoint                   | Description                                            |
| ------ | -------------------------- | ------------------------------------------------------ |
| GET    | `/api/orders/top-spenders` | Menampilkan pelanggan dengan total transaksi tertinggi |

#### Example

```bash
curl -X GET \
"http://localhost:8082/api/orders/reports/top-spenders"
```

---

# 🔄 Event Flow

```text
Client
   │
   ▼
Order Service
   │
   ├── Save Order
   │
   └── Publish OrderCreated Event
                │
                ▼
            Kafka
                │
                ▼
       Catalog Service
                │
                └── Reduce Product Stock
```

---

# 🧠 Technical Decisions

## Database per Service

Setiap service memiliki database sendiri untuk menjaga:

* Service isolation
* Independent deployment
* Loose coupling
* Scalability

---

## Transactional Outbox & Eventual Consistency

Saat order berhasil dibuat:

1. Order disimpan ke database.
2. Event dipublikasikan ke Kafka.
3. Catalog Service mengonsumsi event tersebut.
4. Stok produk dikurangi secara asinkron.
5. Jika terjadi kegagalan, mekanisme retry akan memastikan konsistensi data.

---

## Native SQL Analytics

Endpoint **Top Spenders** menggunakan SQL Window Function:

```sql
DENSE_RANK()
```

Pendekatan ini lebih efisien dibandingkan melakukan agregasi dan ranking di level aplikasi Java.

---

## BigDecimal Precision

Seluruh nilai moneter menggunakan:

```java
BigDecimal
```

untuk memastikan:

* Presisi tinggi
* Tidak ada floating-point rounding error
* Konsistensi perhitungan keuangan

---

# 🗄️ Tech Stack

| Layer            | Technology              |
| ---------------- | ----------------------- |
| Backend          | Spring Boot 3           |
| Database         | PostgreSQL              |
| Cache            | Redis                   |
| Search Engine    | Elasticsearch           |
| Messaging        | Apache Kafka            |
| Containerization | Docker & Docker Compose |
| Build Tool       | Maven                   |
| Java Version     | JDK 21                  |

---

# 🛡️ Troubleshooting

## Docker Container Crash

Periksa log container:

```bash
docker compose logs -f <container_name>
```

---

## Kafka Connectivity Issues

Pastikan container Kafka dalam kondisi:

```bash
healthy
```

Periksa status:

```bash
docker compose ps
```

---

## Elasticsearch Out of Memory

Jika Elasticsearch berhenti secara tiba-tiba:

* Kurangi heap size Elasticsearch
* Sesuaikan konfigurasi memori pada `docker-compose.yml`
* Pastikan RAM host mencukupi

Contoh:

```yaml
ES_JAVA_OPTS: "-Xms512m -Xmx512m"
```

---

# 📈 Future Improvements

* API Gateway
* Service Discovery
* Distributed Tracing (OpenTelemetry)
* Circuit Breaker (Resilience4j)
* Authentication & Authorization (JWT/OAuth2)
* CI/CD Pipeline
* Kubernetes Deployment
* Monitoring dengan Prometheus & Grafana

---

## 📄 License

This project is intended for educational and assessment purposes.

---

⭐ Dibangun menggunakan praktik modern **Microservices Architecture**, **Event-Driven Design**, dan **Cloud-Native Development** untuk menghasilkan sistem yang scalable, resilient, dan maintainable.
