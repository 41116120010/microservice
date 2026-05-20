# LAPORAN KESELURUHAN
## Tugas Harian Mata Kuliah Pemrograman Microservice - Semester 4

**Nama:** Daffiq Trie Octorino  
**Package:** com.daffiqtrie  
**Jumlah Project:** 8 Project Spring Boot

---

## 1. Ringkasan Seluruh Project

| No | Project | Port | Fungsi | Teknologi Kunci |
|----|---------|------|--------|-----------------|
| 1 | **praktikum1** | 8080 | Hello World REST API | Spring Boot, Actuator |
| 2 | **eureka** | 8761 | Service Discovery Server | Netflix Eureka Server |
| 3 | **gateway** | 9310 | API Gateway / Routing | Spring Cloud Gateway |
| 4 | **Produk** | 8081 | CRUD Produk | JPA, H2, Eureka Client |
| 5 | **Pelanggan** | 8083 | CRUD Pelanggan | JPA, H2, Eureka Client |
| 6 | **Order** | 8082 | CRUD Order + Inter-service Comm | JPA, H2, Eureka, RabbitMQ, RestTemplate |
| 7 | **produser** | 8080 | Message Producer | RabbitMQ (AMQP) |
| 8 | **konsumer** | - | Message Consumer + Email | RabbitMQ, Spring Mail |

---

## 2. Arsitektur Keseluruhan

```
                        ┌─────────────────────────────────────────────┐
                        │              CLIENT / BROWSER                │
                        └─────────────────────┬───────────────────────┘
                                              │
                                              ▼
                        ┌─────────────────────────────────────────────┐
                        │          API GATEWAY (Port 9310)             │
                        │   Routing: /api/produk/** → PRODUK          │
                        │            /api/order/**  → ORDER            │
                        └──────┬──────────────────────────┬───────────┘
                               │                          │
                    ┌──────────▼──────────┐    ┌──────────▼──────────┐
                    │  PRODUK SERVICE     │    │   ORDER SERVICE      │
                    │  (Port 8081)        │    │   (Port 8082)        │
                    │  - CRUD Produk      │◄───│   - CRUD Order       │
                    │  - H2 Database      │    │   - H2 Database      │
                    │                     │    │   - RestTemplate      │
                    └──────────┬──────────┘    │   - RabbitMQ Producer│
                               │               └──────────┬───────────┘
                               │                          │
                    ┌──────────▼──────────────────────────▼───────────┐
                    │            EUREKA SERVER (Port 8761)             │
                    │         Service Registry & Discovery             │
                    └─────────────────────────────────────────────────┘
                                                          │
                    ┌─────────────────────────────────────▼───────────┐
                    │              PELANGGAN SERVICE (Port 8083)       │
                    │              - CRUD Pelanggan                    │
                    │              - H2 Database                       │
                    └─────────────────────────────────────────────────┘

    ┌─────────────────────────────────────────────────────────────────┐
    │                    RABBITMQ (Port 5672)                          │
    │                    Queue: "myQueue"                              │
    └──────────┬──────────────────────────────────┬───────────────────┘
               │                                  │
    ┌──────────▼──────────┐            ┌──────────▼──────────┐
    │  PRODUSER SERVICE   │            │  KONSUMER SERVICE    │
    │  (Port 8080)        │            │  (No Web Port)       │
    │  - REST → Queue     │            │  - Queue → Email     │
    └─────────────────────┘            │  - Gmail SMTP        │
                                       └──────────────────────┘
```

---

## 3. Konsep Microservice yang Dipelajari

### 3.1 Praktikum 1 - Pengenalan Spring Boot
- Membuat REST API sederhana
- Memahami annotation `@SpringBootApplication`, `@RestController`, `@GetMapping`
- Menggunakan `@RequestParam` untuk query parameter
- Spring Boot Actuator untuk monitoring

### 3.2 Service Discovery (Eureka)
- Konsep service registry
- Eureka Server sebagai "buku telepon" microservice
- Konfigurasi server (tidak register diri sendiri)
- Dashboard monitoring service

### 3.3 API Gateway
- Single entry point untuk semua microservice
- Routing berdasarkan path pattern
- Load balancing dengan prefix `lb://`
- Integrasi dengan Eureka untuk dynamic routing

### 3.4 CRUD Microservice (Produk & Pelanggan)
- Arsitektur layered: Controller → Service → Repository → Database
- Spring Data JPA untuk ORM
- H2 Database (embedded)
- Lombok untuk mengurangi boilerplate
- Eureka Client registration

### 3.5 Inter-Service Communication (Order)
- RestTemplate untuk HTTP call antar service
- DiscoveryClient untuk menemukan service
- Value Object (VO) pattern untuk data dari service lain
- JPA Lifecycle Callbacks (`@PrePersist`, `@PreUpdate`)
- Custom repository methods

### 3.6 Message Queue (Produser & Konsumer)
- RabbitMQ sebagai message broker
- Producer-Consumer pattern
- `@RabbitListener` untuk asynchronous message consumption
- Event-driven architecture

### 3.7 Email Integration (Konsumer)
- Spring Mail dengan JavaMailSender
- Gmail SMTP configuration
- App Password untuk autentikasi
- String parsing untuk extract data dari pesan

### 3.8 Containerization (Docker)
- Dockerfile untuk setiap service
- Docker network untuk komunikasi antar container
- Port mapping dan expose

---

## 4. Teknologi yang Digunakan

| Teknologi | Versi | Fungsi |
|-----------|-------|--------|
| Java | 17 | Bahasa pemrograman |
| Spring Boot | 4.0.3 - 4.0.5 | Framework utama |
| Spring Cloud | 2025.1.0 - 2025.1.1 | Microservice tools |
| Netflix Eureka | (via Spring Cloud) | Service Discovery |
| Spring Cloud Gateway | (via Spring Cloud) | API Gateway |
| Spring Data JPA | (via Spring Boot) | ORM / Database access |
| H2 Database | (runtime) | Embedded database |
| Lombok | (compile-time) | Code generation |
| RabbitMQ | (external) | Message broker |
| Spring AMQP | (via Spring Boot) | RabbitMQ integration |
| Spring Mail | (via Spring Boot) | Email sending |
| Maven | (wrapper) | Build tool |
| Docker | (external) | Containerization |

---

## 5. Urutan Menjalankan Service

Untuk menjalankan seluruh arsitektur microservice:

1. **RabbitMQ** → Jalankan RabbitMQ server terlebih dahulu
2. **Eureka Server** → `cd eureka && ./mvnw spring-boot:run`
3. **Produk Service** → `cd Produk && ./mvnw spring-boot:run`
4. **Pelanggan Service** → `cd Pelanggan && ./mvnw spring-boot:run`
5. **Order Service** → `cd Order && ./mvnw spring-boot:run`
6. **Gateway** → `cd gateway && ./mvnw spring-boot:run`
7. **Konsumer** → `cd konsumer && ./mvnw spring-boot:run`

---

## 6. Port Mapping

| Service | Port | Protokol |
|---------|------|----------|
| Eureka Server | 8761 | HTTP (Dashboard + REST) |
| Produk | 8081 | HTTP (REST API) |
| Order | 8082 | HTTP (REST API) |
| Pelanggan | 8083 | HTTP (REST API) |
| Gateway | 9310 | HTTP (Proxy) |
| RabbitMQ | 5672 | AMQP |
| RabbitMQ Management | 15672 | HTTP (Dashboard) |
| Gmail SMTP | 587 | SMTP + TLS |

---

## 7. Daftar File Laporan

| File | Project |
|------|---------|
| `LAPORAN_PRAKTIKUM1.md` | Project 1 - Hello World |
| `LAPORAN_EUREKA.md` | Project 2 - Eureka Server |
| `LAPORAN_GATEWAY.md` | Project 3 - API Gateway |
| `LAPORAN_PRODUK.md` | Project 4 - Produk Service |
| `LAPORAN_PELANGGAN.md` | Project 5 - Pelanggan Service |
| `LAPORAN_ORDER.md` | Project 6 - Order Service |
| `LAPORAN_PRODUSER.md` | Project 7 - Message Producer |
| `LAPORAN_KONSUMER.md` | Project 8 - Message Consumer |
| `LAPORAN_KESELURUHAN.md` | Ringkasan keseluruhan (file ini) |
