# LAPORAN PROJECT 3: GATEWAY
## API Gateway dengan Spring Cloud Gateway

---

## 1. Deskripsi Umum

Project **gateway** merupakan implementasi **API Gateway** menggunakan Spring Cloud Gateway. API Gateway berfungsi sebagai pintu masuk tunggal (single entry point) untuk semua request dari client ke microservice. Alih-alih client harus mengetahui alamat setiap microservice, client cukup mengirim request ke Gateway, dan Gateway akan meneruskan (routing) request tersebut ke service yang tepat.

**Fungsi Utama:**
- Single entry point untuk semua microservice
- Routing request berdasarkan path URL
- Load balancing menggunakan Eureka (prefix `lb://`)
- Menyederhanakan komunikasi client-to-service

---

## 2. Arsitektur & Flow Aplikasi

```
[Client/Browser]
       |
       | Semua request masuk ke port 9310
       v
[API Gateway - Port 9310]
       |
       |--- /api/produk/**  ---> [Produk Service - Port 8081]
       |
       |--- /api/order/**   ---> [Order Service - Port 8082]
       |
       v
[Eureka Server - Port 8761]
  (untuk service discovery & load balancing)
```

**Flow Detail:**
1. Client mengirim request ke `http://localhost:9310/api/produk/1`
2. Gateway menerima request dan mencocokkan path dengan routing rules
3. Path `/api/produk/**` cocok dengan route "produk-service"
4. Gateway bertanya ke Eureka: "Di mana service PRODUK?"
5. Eureka memberikan alamat service Produk (misal: `http://172.18.0.3:8081`)
6. Gateway meneruskan request ke `http://172.18.0.3:8081/api/produk/1`
7. Response dari Produk Service diteruskan kembali ke client

---

## 3. Struktur Project

```
gateway/
├── src/main/java/com/daffiqtrie/gateway/
│   └── GatewayApplication.java
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── dockerfile
```

---

## 4. Analisa File per File

### 4.1 pom.xml

| Baris | Kode | Penjelasan |
|-------|------|------------|
| 1-3 | `<?xml ...><project ...>` | Header XML dan root element POM |
| 5-9 | `<parent>spring-boot-starter-parent 4.0.5</parent>` | Parent POM Spring Boot versi 4.0.5 |
| 10 | `<groupId>com.daffiqtrie</groupId>` | Group ID |
| 11 | `<artifactId>gateway</artifactId>` | Nama artifact |
| 12 | `<version>0.0.1-SNAPSHOT</version>` | Versi development |
| 16 | `<java.version>17</java.version>` | Java 17 |
| 17 | `<spring-cloud.version>2025.1.1</spring-cloud.version>` | Versi Spring Cloud |
| 20-22 | `spring-cloud-starter-gateway-server-webmvc` | **Dependency utama** - Spring Cloud Gateway berbasis WebMVC |
| 23-25 | `spring-cloud-starter-netflix-eureka-client` | Eureka Client untuk service discovery |
| 27-30 | `spring-boot-starter-test` | Dependency testing |
| 32-40 | `<dependencyManagement>` | BOM Spring Cloud untuk manajemen versi |
| 44-46 | `spring-boot-maven-plugin` | Plugin build |

**Dependency Kunci:**
- `spring-cloud-starter-gateway-server-webmvc` → Menyediakan fitur routing, filtering, dan load balancing berbasis Spring MVC
- `spring-cloud-starter-netflix-eureka-client` → Memungkinkan Gateway menemukan service melalui Eureka

---

### 4.2 application.properties

```properties
server.port=9310
```
**Baris 1:** Mengatur port Gateway ke **9310** (port custom agar tidak bentrok dengan service lain)

```properties
spring.application.name=gateway
```
**Baris 3:** Nama aplikasi yang terdaftar di Eureka = "gateway"

```properties
spring.cloud.gateway.server.webmvc.routes[0].id=produk-service
spring.cloud.gateway.server.webmvc.routes[0].uri=lb://PRODUK
spring.cloud.gateway.server.webmvc.routes[0].predicates[0]=Path=/api/produk/**
```
**Baris 5-7:** Konfigurasi Route pertama:
- `routes[0].id=produk-service` → Identifier route = "produk-service"
- `routes[0].uri=lb://PRODUK` → Target URI menggunakan load balancer (`lb://`) ke service bernama "PRODUK" di Eureka
- `routes[0].predicates[0]=Path=/api/produk/**` → Route ini aktif jika path request cocok dengan `/api/produk/**` (wildcard)

```properties
spring.cloud.gateway.server.webmvc.routes[1].id=order-service
spring.cloud.gateway.server.webmvc.routes[1].uri=lb://ORDER
spring.cloud.gateway.server.webmvc.routes[1].predicates[0]=Path=/api/order/**
```
**Baris 9-11:** Konfigurasi Route kedua:
- `routes[1].id=order-service` → Identifier route = "order-service"
- `routes[1].uri=lb://ORDER` → Target ke service "ORDER" via load balancer
- `routes[1].predicates[0]=Path=/api/order/**` → Aktif untuk path `/api/order/**`

```properties
eureka.client.register-with-eureka=true
eureka.client.fetch-registry=true
eureka.client.service-url.defaultZone=http://172.18.0.2:8761/eureka/
eureka.instance.hostname=localhost
```
**Baris 13-16:** Konfigurasi Eureka Client:
- `register-with-eureka=true` → Gateway mendaftarkan diri ke Eureka
- `fetch-registry=true` → Gateway mengambil daftar service dari Eureka (untuk routing)
- `service-url.defaultZone` → Alamat Eureka Server (IP Docker network)
- `instance.hostname=localhost` → Hostname yang didaftarkan ke Eureka

---

### 4.3 GatewayApplication.java

```java
package com.daffiqtrie.gateway;
```
**Baris 1:** Deklarasi package

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
```
**Baris 3-4:** Import:
- `SpringApplication` → Untuk menjalankan aplikasi
- `@SpringBootApplication` → Annotation utama Spring Boot

```java
@SpringBootApplication
public class GatewayApplication {
```
**Baris 6-7:**
- `@SpringBootApplication` → Mengaktifkan auto-configuration. Spring Cloud Gateway secara otomatis dikonfigurasi berdasarkan dependency dan properties yang ada

```java
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
```
**Baris 9-11:** Method main - entry point aplikasi

**Catatan:** Tidak ada annotation tambahan seperti `@EnableDiscoveryClient` karena pada versi Spring Cloud terbaru, Eureka Client otomatis aktif jika dependency-nya ada di classpath.

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `args` | `String[]` | Argumen command line |

---

### 4.4 dockerfile

```dockerfile
FROM openjdk:27-ea-17-jdk-slim-trixie
```
**Baris 1:** Base image OpenJDK 17

```dockerfile
WORKDIR /app
```
**Baris 2:** Working directory = `/app`

```dockerfile
COPY target/gateway-0.0.1-SNAPSHOT.jar /app/app.jar
```
**Baris 3:** Copy JAR file ke container

```dockerfile
EXPOSE 9310
```
**Baris 4:** Expose port 9310

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```
**Baris 5:** Jalankan aplikasi

---

## 5. Routing Table

| Path Pattern | Target Service | Eureka Name | Port |
|-------------|----------------|-------------|------|
| `/api/produk/**` | Produk Service | PRODUK | 8081 |
| `/api/order/**` | Order Service | ORDER | 8082 |

## 6. Contoh Penggunaan

```
# Akses Produk melalui Gateway
GET http://localhost:9310/api/produk
→ diteruskan ke → http://PRODUK-SERVICE:8081/api/produk

# Akses Order melalui Gateway
GET http://localhost:9310/api/order
→ diteruskan ke → http://ORDER-SERVICE:8082/api/order
```

## 7. Hubungan dengan Project Lain

- **Bergantung pada:** Eureka Server (untuk service discovery)
- **Meneruskan request ke:** Produk Service, Order Service
- **Diakses oleh:** Client/Frontend
