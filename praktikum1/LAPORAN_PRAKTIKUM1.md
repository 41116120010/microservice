# LAPORAN PROJECT 1: PRAKTIKUM1
## Pengenalan Spring Boot - Hello World REST API

---

## 1. Deskripsi Umum

Project **praktikum1** merupakan project pengenalan dasar Spring Boot yang membuat sebuah REST API sederhana dengan endpoint `/hello`. Project ini mendemonstrasikan cara membuat aplikasi web minimal menggunakan Spring Boot dengan fitur:
- REST Controller
- Request Parameter
- Spring Boot Actuator untuk monitoring

---

## 2. Arsitektur & Flow Aplikasi

```
[Client/Browser] 
       |
       | HTTP GET /hello?name=Daffiq
       v
[Spring Boot Application - Port default 8080]
       |
       v
[Praktikum1Application (@RestController)]
       |
       | return "Hello Daffiq!"
       v
[Client/Browser menerima response]
```

**Flow:**
1. Client mengirim HTTP GET request ke endpoint `/hello`
2. Spring Boot menerima request dan meneruskan ke method `hello()`
3. Method membaca parameter `name` (default: "World")
4. Method mengembalikan string "Hello {name}!"
5. Response dikirim kembali ke client

---

## 3. Struktur Project

```
praktikum1/
├── src/main/java/com/daffiqtrie/praktikum1/
│   └── Praktikum1Application.java
├── src/main/resources/
│   └── application.yaml
├── pom.xml
└── docker-compose.yml (kosong)
```

---

## 4. Analisa File per File

### 4.1 pom.xml (Project Object Model)

File konfigurasi Maven yang mendefinisikan dependency dan build project.

| Baris | Kode | Penjelasan |
|-------|------|------------|
| 1 | `<?xml version="1.0" encoding="UTF-8"?>` | Deklarasi XML dengan encoding UTF-8 |
| 2-3 | `<project xmlns=...>` | Root element POM dengan namespace Maven |
| 4 | `<modelVersion>4.0.0</modelVersion>` | Versi model POM yang digunakan |
| 5-9 | `<parent>...</parent>` | Menggunakan parent `spring-boot-starter-parent` versi 4.0.3 sebagai basis konfigurasi |
| 11 | `<groupId>com.daffiqtrie</groupId>` | Group ID project (identitas organisasi) |
| 12 | `<artifactId>praktikum1</artifactId>` | Artifact ID (nama project) |
| 13 | `<version>0.0.1-SNAPSHOT</version>` | Versi project (SNAPSHOT = versi development) |
| 16 | `<java.version>17</java.version>` | Menggunakan Java 17 |
| 19-21 | `spring-boot-starter-actuator` | Dependency untuk monitoring & health check endpoint |
| 23-25 | `spring-boot-starter-webmvc` | Dependency untuk membuat REST API (Spring MVC) |
| 27-30 | `spring-boot-starter-webmvc-test` | Dependency testing untuk web layer (scope test) |
| 34-36 | `spring-boot-maven-plugin` | Plugin untuk packaging aplikasi menjadi executable JAR |

**Dependency yang digunakan:**
- `spring-boot-starter-actuator` → Menyediakan endpoint monitoring seperti `/actuator/health`
- `spring-boot-starter-webmvc` → Menyediakan embedded Tomcat server dan Spring MVC framework
- `spring-boot-starter-webmvc-test` → Menyediakan tools testing untuk controller

---

### 4.2 application.yaml

File konfigurasi aplikasi Spring Boot.

| Baris | Kode | Penjelasan |
|-------|------|------------|
| 1 | `spring:` | Root konfigurasi Spring |
| 2 | `  application:` | Konfigurasi aplikasi |
| 3 | `    name: praktikum1` | Nama aplikasi yang terdaftar = "praktikum1" |

---

### 4.3 Praktikum1Application.java

File utama aplikasi yang berfungsi sebagai entry point dan REST controller.

```java
package com.daffiqtrie.praktikum1;
```
**Baris 1:** Deklarasi package. Menentukan lokasi class dalam struktur project.

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
```
**Baris 3-7:** Import statement:
- `SpringApplication` → Class untuk menjalankan aplikasi Spring Boot
- `@SpringBootApplication` → Annotation gabungan dari @Configuration, @EnableAutoConfiguration, @ComponentScan
- `@GetMapping` → Annotation untuk mapping HTTP GET request
- `@RequestParam` → Annotation untuk mengambil query parameter dari URL
- `@RestController` → Annotation yang menandai class sebagai REST controller (gabungan @Controller + @ResponseBody)

```java
@SpringBootApplication
@RestController
public class Praktikum1Application {
```
**Baris 9-11:**
- `@SpringBootApplication` → Mengaktifkan auto-configuration, component scanning, dan konfigurasi Spring Boot
- `@RestController` → Menandai class ini sebagai REST controller yang mengembalikan data langsung (bukan view)
- `Praktikum1Application` → Nama class utama aplikasi

```java
    public static void main(String[] args) {
        SpringApplication.run(Praktikum1Application.class, args);
    }
```
**Baris 13-15:** Method `main()`:
- Entry point aplikasi Java
- `SpringApplication.run()` → Memulai Spring Boot application context, menjalankan embedded web server, dan melakukan auto-configuration

```java
    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return String.format("Hello %s!", name);
    }
```
**Baris 17-20:** Method `hello()`:
- `@GetMapping("/hello")` → Memetakan HTTP GET request pada path `/hello` ke method ini
- `@RequestParam(value = "name", defaultValue = "World")` → Mengambil parameter `name` dari query string. Jika tidak ada, gunakan default "World"
- `String name` → Variabel yang menyimpan nilai parameter name
- `String.format("Hello %s!", name)` → Membuat string response dengan format "Hello {name}!"
- Return type `String` → Response body yang dikirim ke client

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `args` | `String[]` | Argumen command line saat menjalankan aplikasi |
| `name` | `String` | Menyimpan nama dari query parameter (default: "World") |

---

## 5. Cara Menjalankan

```bash
./mvnw spring-boot:run
```

## 6. Endpoint yang Tersedia

| Method | Endpoint | Parameter | Response |
|--------|----------|-----------|----------|
| GET | `/hello` | `name` (optional, default: "World") | "Hello {name}!" |
| GET | `/actuator/health` | - | Status kesehatan aplikasi |

## 7. Contoh Penggunaan

```
GET http://localhost:8080/hello
Response: "Hello World!"

GET http://localhost:8080/hello?name=Daffiq
Response: "Hello Daffiq!"
```
