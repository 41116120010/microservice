# LAPORAN PROJECT 2: EUREKA
## Service Discovery Server dengan Netflix Eureka

---

## 1. Deskripsi Umum

Project **eureka** merupakan implementasi **Service Discovery Server** menggunakan Netflix Eureka. Dalam arsitektur microservice, Eureka Server berfungsi sebagai "buku telepon" yang menyimpan informasi lokasi (host dan port) dari semua microservice yang terdaftar. Dengan adanya Eureka, setiap service tidak perlu mengetahui alamat IP/port service lain secara hardcode — cukup menanyakan ke Eureka Server.

**Fungsi Utama:**
- Menyediakan registry untuk semua microservice
- Memungkinkan service saling menemukan satu sama lain (service discovery)
- Memonitor status kesehatan (health) setiap service yang terdaftar
- Menyediakan dashboard web untuk melihat service yang aktif

---

## 2. Arsitektur & Flow Aplikasi

```
[Eureka Server - Port 8761]
       ^         ^         ^
       |         |         |
   register  register  register
       |         |         |
[Produk]   [Order]   [Pelanggan]  [Gateway]
 :8081      :8082      :8083       :9310

Flow:
1. Eureka Server dijalankan terlebih dahulu
2. Setiap microservice (client) mendaftarkan dirinya ke Eureka
3. Eureka menyimpan informasi service (nama, host, port, status)
4. Ketika service A ingin berkomunikasi dengan service B,
   service A bertanya ke Eureka: "Di mana service B?"
5. Eureka memberikan alamat service B
6. Service A berkomunikasi langsung ke service B
```

---

## 3. Struktur Project

```
eureka/
├── src/main/java/com/daffiqtrie/eureka/
│   └── EurekaApplication.java
├── src/main/resources/
│   └── application.yaml
├── pom.xml
└── dockerfile
```

---

## 4. Analisa File per File

### 4.1 pom.xml

| Baris | Kode | Penjelasan |
|-------|------|------------|
| 1-3 | `<?xml ...><project ...>` | Header XML dan root element POM |
| 4 | `<modelVersion>4.0.0</modelVersion>` | Versi model POM |
| 5-9 | `<parent>spring-boot-starter-parent 4.0.3</parent>` | Parent POM Spring Boot versi 4.0.3 |
| 10 | `<groupId>com.daffiqtrie</groupId>` | Group ID project |
| 11 | `<artifactId>eureka</artifactId>` | Nama artifact |
| 12 | `<version>0.0.1-SNAPSHOT</version>` | Versi development |
| 13 | `<description>Praktikum 11 Maret 2025</description>` | Deskripsi - tanggal praktikum |
| 16 | `<java.version>17</java.version>` | Java 17 |
| 17 | `<spring-cloud.version>2025.1.0</spring-cloud.version>` | Versi Spring Cloud yang digunakan |
| 20-22 | `spring-cloud-starter-netflix-eureka-server` | **Dependency utama** - menyediakan Eureka Server |
| 24-27 | `spring-boot-starter-test` | Dependency testing |
| 29-37 | `<dependencyManagement>` | Mengimpor BOM (Bill of Materials) Spring Cloud untuk manajemen versi dependency |
| 41-43 | `spring-boot-maven-plugin` | Plugin untuk build executable JAR |

**Dependency Kunci:**
- `spring-cloud-starter-netflix-eureka-server` → Library yang menyediakan semua komponen Eureka Server termasuk dashboard web, REST API registry, dan heartbeat mechanism

---

### 4.2 application.yaml

```yaml
spring:
  application:
    name: eureka
```
**Baris 1-3:** Mendefinisikan nama aplikasi sebagai "eureka"

```yaml
server:
  port: 8761
```
**Baris 5-6:** Mengatur port server ke **8761** (port standar Eureka Server)

```yaml
eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
```
**Baris 8-10:** Konfigurasi Eureka Client:
- `register-with-eureka: false` → Eureka Server TIDAK mendaftarkan dirinya sendiri ke registry (karena dia sendiri adalah registry)
- `fetch-registry: false` → Eureka Server TIDAK perlu mengambil registry dari server lain (karena dia adalah satu-satunya server)

```yaml
  logging:
    level:
      com.netflix.eureka: OFF
      com.netflix.discovery: OFF
```
**Baris 11-14:** Mematikan logging dari package Netflix Eureka dan Discovery untuk mengurangi noise di console

---

### 4.3 EurekaApplication.java

```java
package com.daffiqtrie.eureka;
```
**Baris 1:** Deklarasi package

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
```
**Baris 3-5:** Import statement:
- `SpringApplication` → Untuk menjalankan aplikasi Spring Boot
- `@SpringBootApplication` → Annotation utama Spring Boot
- `@EnableEurekaServer` → Annotation yang mengaktifkan fitur Eureka Server

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaApplication {
```
**Baris 7-9:**
- `@SpringBootApplication` → Mengaktifkan auto-configuration dan component scanning
- `@EnableEurekaServer` → **Annotation kunci** yang mengubah aplikasi Spring Boot biasa menjadi Eureka Server. Annotation ini mengaktifkan:
  - REST API untuk registrasi service
  - Dashboard web di `http://localhost:8761`
  - Heartbeat mechanism untuk monitoring service
  - Self-preservation mode

```java
    public static void main(String[] args) {
        SpringApplication.run(EurekaApplication.class, args);
    }
```
**Baris 11-13:** Method main - entry point yang menjalankan Eureka Server

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `args` | `String[]` | Argumen command line |

---

### 4.4 dockerfile

```dockerfile
FROM openjdk:27-ea-17-jdk-slim-trixie
```
**Baris 1:** Base image menggunakan OpenJDK 17 (slim variant dari Debian Trixie)

```dockerfile
WORKDIR /app
```
**Baris 2:** Mengatur working directory di dalam container ke `/app`

```dockerfile
COPY target/eureka-0.0.1-SNAPSHOT.jar /app/app.jar
```
**Baris 3:** Menyalin file JAR hasil build ke dalam container

```dockerfile
EXPOSE 8761
```
**Baris 4:** Mendeklarasikan bahwa container menggunakan port 8761

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
```
**Baris 5:** Perintah yang dijalankan saat container start - menjalankan aplikasi Java

---

## 5. Cara Menjalankan

```bash
# Build JAR
./mvnw clean package -DskipTests

# Jalankan langsung
./mvnw spring-boot:run

# Atau dengan Docker
docker build -t eureka-server .
docker run -p 8761:8761 eureka-server
```

## 6. Dashboard & Endpoint

| Endpoint | Fungsi |
|----------|--------|
| `http://localhost:8761` | Dashboard Eureka (Web UI) |
| `http://localhost:8761/eureka/apps` | REST API - daftar semua service terdaftar |

## 7. Hubungan dengan Project Lain

Eureka Server adalah **fondasi** dari arsitektur microservice ini. Semua service lain (Produk, Order, Pelanggan, Gateway) mendaftarkan diri ke Eureka Server ini agar bisa saling berkomunikasi.
