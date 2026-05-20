# LAPORAN PROJECT 4: PRODUK
## Microservice Manajemen Produk (CRUD + Eureka Client)

---

## 1. Deskripsi Umum

Project **Produk** merupakan microservice yang menangani manajemen data produk. Service ini menyediakan REST API untuk operasi CRUD (Create, Read, Update, Delete) pada entitas Produk. Data disimpan menggunakan database H2 (in-memory/file-based) dan service ini terdaftar di Eureka Server untuk service discovery.

**Fungsi Utama:**
- Menyimpan dan mengelola data produk (nama, satuan, harga)
- Menyediakan REST API CRUD
- Terdaftar di Eureka untuk ditemukan oleh service lain (terutama Order Service)

---

## 2. Arsitektur & Flow Aplikasi

```
[Client / Gateway / Order Service]
       |
       | HTTP Request
       v
[ProdukController - @RestController]
       |
       | Memanggil method service
       v
[ProdukService - @Service]
       |
       | Memanggil method repository
       v
[ProdukRepository - JpaRepository]
       |
       | SQL Query (auto-generated oleh JPA)
       v
[H2 Database - file ~/produk]
```

**Arsitektur Layer:**
1. **Controller Layer** → Menerima HTTP request, validasi input, return response
2. **Service Layer** → Business logic (pada project ini sederhana, hanya meneruskan ke repository)
3. **Repository Layer** → Akses database menggunakan Spring Data JPA
4. **Model Layer** → Definisi entitas/tabel database

---

## 3. Struktur Project

```
Produk/
├── src/main/java/com/daffiqtrie/Produk/
│   ├── ProdukApplication.java
│   ├── controller/
│   │   └── ProdukController.java
│   ├── model/
│   │   └── Produk.java
│   ├── repository/
│   │   └── ProdukRepository.java
│   └── service/
│       └── ProdukService.java
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── dockerfile
```

---

## 4. Analisa File per File

### 4.1 pom.xml

| Dependency | Fungsi |
|-----------|--------|
| `spring-boot-h2console` | Menyediakan web console untuk mengakses database H2 |
| `spring-boot-starter-data-jpa` | Spring Data JPA untuk ORM (Object Relational Mapping) |
| `spring-boot-starter-webmvc` | Spring MVC untuk REST API |
| `spring-boot-devtools` | Hot reload saat development |
| `h2` (runtime) | Database H2 embedded |
| `lombok` | Mengurangi boilerplate code (getter, setter, dll) |
| `spring-cloud-starter-netflix-eureka-client` | Mendaftarkan service ke Eureka Server |

**Spring Cloud Version:** 2025.1.0

**Build Plugins:**
- `maven-compiler-plugin` → Konfigurasi annotation processor untuk Lombok
- `spring-boot-maven-plugin` → Build executable JAR (exclude Lombok dari JAR final)

---

### 4.2 application.properties

```properties
spring.application.name=Produk
```
**Baris 1:** Nama service yang terdaftar di Eureka = "Produk" (akan menjadi "PRODUK" di Eureka)

```properties
server.port=8081
```
**Baris 2:** Port aplikasi = 8081

```properties
spring.datasource.url=jdbc:h2:~/produk;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=12345
```
**Baris 4-7:** Konfigurasi datasource:
- `url` → Lokasi file database H2 di home directory (`~/produk`), `DB_CLOSE_DELAY=-1` agar database tidak ditutup saat tidak ada koneksi
- `driverClassName` → Driver JDBC untuk H2
- `username` → Username database = "sa" (default H2)
- `password` → Password database = "12345"

```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```
**Baris 8-9:** Mengaktifkan H2 Console (web UI) di path `/h2-console`

```properties
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
```
**Baris 10-11:**
- `database-platform` → Dialect Hibernate untuk H2 (menghasilkan SQL yang kompatibel dengan H2)
- `ddl-auto=update` → Hibernate otomatis membuat/mengupdate tabel berdasarkan entity class

```properties
eureka.client.serviceUrl.defaultZone=http://172.18.0.2:8761/eureka/
```
**Baris 12:** Alamat Eureka Server untuk registrasi service

---

### 4.3 Produk.java (Model/Entity)

```java
package com.daffiqtrie.Produk.model;
```
**Baris 1:** Package model - berisi definisi entitas database

```java
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
```
**Baris 3-7:** Import:
- `@Entity` → Menandai class sebagai entitas JPA (akan dipetakan ke tabel database)
- `@GeneratedValue` → Strategi generate nilai otomatis untuk primary key
- `@GenerationType` → Enum tipe strategi generate (IDENTITY, SEQUENCE, dll)
- `@Id` → Menandai field sebagai primary key
- `@Data` → Lombok annotation yang otomatis generate getter, setter, toString, equals, hashCode

```java
@Entity
@Data
public class Produk {
```
**Baris 9-11:**
- `@Entity` → Class ini merepresentasikan tabel "produk" di database
- `@Data` → Lombok otomatis membuat semua getter/setter dan method utility

```java
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
```
**Baris 12-14:**
- `@Id` → Field `id` adalah primary key
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` → Nilai ID di-generate otomatis oleh database (auto-increment)
- `private Long id` → Tipe Long untuk ID produk

```java
    private String nama;
    private String satuan;
    private Long harga;
```
**Baris 15-17:** Field/kolom tabel:
- `nama` → Nama produk (String)
- `satuan` → Satuan produk, misal "pcs", "kg" (String)
- `harga` → Harga produk dalam Long (angka bulat)

**Variabel/Field:**
| Field | Tipe | Fungsi | Kolom DB |
|-------|------|--------|----------|
| `id` | `Long` | Primary key, auto-increment | `id` |
| `nama` | `String` | Nama produk | `nama` |
| `satuan` | `String` | Satuan produk | `satuan` |
| `harga` | `Long` | Harga produk | `harga` |

---

### 4.4 ProdukRepository.java

```java
package com.daffiqtrie.Produk.repository;
```
**Baris 1:** Package repository

```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.daffiqtrie.Produk.model.Produk;
```
**Baris 3-5:** Import:
- `JpaRepository` → Interface Spring Data JPA yang menyediakan method CRUD otomatis
- `@Repository` → Menandai interface sebagai repository (komponen data access)
- `Produk` → Entity class yang dikelola

```java
@Repository
public interface ProdukRepository extends JpaRepository<Produk, Long> {
}
```
**Baris 7-9:**
- `@Repository` → Spring mengenali ini sebagai bean repository
- `extends JpaRepository<Produk, Long>` → Mewarisi semua method CRUD dari JpaRepository:
  - `findAll()` → SELECT * FROM produk
  - `findById(Long id)` → SELECT * FROM produk WHERE id = ?
  - `save(Produk)` → INSERT/UPDATE
  - `deleteById(Long id)` → DELETE FROM produk WHERE id = ?
- Generic parameter: `Produk` = tipe entity, `Long` = tipe primary key

---

### 4.5 ProdukService.java

```java
package com.daffiqtrie.Produk.service;
```
**Baris 1:** Package service

```java
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.daffiqtrie.Produk.repository.ProdukRepository;
import com.daffiqtrie.Produk.model.Produk;
```
**Baris 3-7:** Import:
- `List` → Collection untuk menampung daftar produk
- `@Autowired` → Dependency injection otomatis
- `@Service` → Menandai class sebagai service layer
- `ProdukRepository` → Repository yang digunakan
- `Produk` → Model entity

```java
@Service
public class ProdukService {
    @Autowired
    private ProdukRepository produkRepository;
```
**Baris 9-12:**
- `@Service` → Spring mengenali class ini sebagai service bean
- `@Autowired` → Spring otomatis meng-inject instance ProdukRepository
- `produkRepository` → Variabel yang menyimpan referensi ke repository

```java
    public List<Produk> getAllProduk() {
        return produkRepository.findAll();
    }
```
**Baris 14-16:** Method `getAllProduk()`:
- Return type: `List<Produk>` → Daftar semua produk
- Memanggil `findAll()` dari JpaRepository → menghasilkan query `SELECT * FROM produk`

```java
    public Produk getProdukById(Long id) {
        return produkRepository.findById(id).orElse(null);
    }
```
**Baris 18-20:** Method `getProdukById()`:
- Parameter: `Long id` → ID produk yang dicari
- `findById(id)` → Mengembalikan `Optional<Produk>`
- `.orElse(null)` → Jika tidak ditemukan, return null

```java
    public Produk createProduk(Produk produk) {
        return produkRepository.save(produk);
    }
```
**Baris 22-24:** Method `createProduk()`:
- Parameter: `Produk produk` → Object produk baru
- `save(produk)` → INSERT ke database, return entity yang sudah tersimpan (dengan ID)

```java
    public void deleteProduk(Long id) {
        produkRepository.deleteById(id);
    }
```
**Baris 26-28:** Method `deleteProduk()`:
- Parameter: `Long id` → ID produk yang akan dihapus
- `deleteById(id)` → DELETE FROM produk WHERE id = ?

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `produkRepository` | `ProdukRepository` | Akses ke database produk |

---

### 4.6 ProdukController.java

```java
package com.daffiqtrie.Produk.controller;
```
**Baris 1:** Package controller

```java
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.daffiqtrie.Produk.model.Produk;
import com.daffiqtrie.Produk.service.ProdukService;
```
**Baris 3-8:** Import:
- `List` → Collection untuk response list
- `@Autowired` → Dependency injection
- `ResponseEntity` → Wrapper untuk HTTP response (status code + body)
- `@RestController, @RequestMapping, @GetMapping, @PostMapping, @DeleteMapping, @PathVariable, @RequestBody` → Annotation REST
- `Produk` → Model
- `ProdukService` → Service layer

```java
@RestController
@RequestMapping("/api/produk")
public class ProdukController {
    @Autowired
    private ProdukService produkService;
```
**Baris 10-14:**
- `@RestController` → Class ini adalah REST controller (response = JSON)
- `@RequestMapping("/api/produk")` → Base path untuk semua endpoint di controller ini
- `@Autowired private ProdukService` → Inject service layer

```java
    @GetMapping
    public List<Produk> getAllProduk() {
        return produkService.getAllProduk();
    }
```
**Baris 16-19:** Endpoint GET `/api/produk`:
- Mengembalikan semua produk dalam format JSON array
- HTTP 200 OK otomatis

```java
    @GetMapping("/{id}")
    public ResponseEntity<Produk> getProdukById(@PathVariable Long id) {
        Produk produk = produkService.getProdukById(id);
        return produk != null ? ResponseEntity.ok(produk) : ResponseEntity.notFound().build();
    }
```
**Baris 21-25:** Endpoint GET `/api/produk/{id}`:
- `@PathVariable Long id` → Mengambil nilai `{id}` dari URL
- Jika produk ditemukan → HTTP 200 + data produk
- Jika tidak ditemukan → HTTP 404 Not Found

```java
    @PostMapping
    public Produk createProduk(@RequestBody Produk produk) {
        return produkService.createProduk(produk);
    }
```
**Baris 27-30:** Endpoint POST `/api/produk`:
- `@RequestBody Produk produk` → Mengambil JSON body dan convert ke object Produk
- Menyimpan produk baru dan mengembalikan data yang tersimpan (dengan ID)
- HTTP 200 OK

```java
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduk(@PathVariable Long id) {
        produkService.deleteProduk(id);
        return ResponseEntity.noContent().build();
    }
```
**Baris 32-36:** Endpoint DELETE `/api/produk/{id}`:
- Menghapus produk berdasarkan ID
- Return HTTP 204 No Content (berhasil tanpa body)

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `produkService` | `ProdukService` | Referensi ke service layer |
| `id` | `Long` | ID produk dari path variable |
| `produk` | `Produk` | Object produk dari request body |

---

### 4.7 ProdukApplication.java

```java
package com.daffiqtrie.Produk;
```
**Baris 1:** Package utama

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
```
**Baris 3-5:** Import:
- `@EnableDiscoveryClient` → Mengaktifkan fitur service discovery (mendaftar ke Eureka)

```java
@SpringBootApplication
@EnableDiscoveryClient
public class ProdukApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProdukApplication.class, args);
    }
}
```
**Baris 7-12:**
- `@SpringBootApplication` → Auto-configuration Spring Boot
- `@EnableDiscoveryClient` → Service ini akan mendaftarkan diri ke Eureka Server saat startup
- `main()` → Entry point aplikasi

---

### 4.8 dockerfile

```dockerfile
FROM openjdk:27-ea-17-jdk-slim-trixie
WORKDIR /app
COPY target/Produk-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- Base image: OpenJDK 17
- Copy JAR hasil build
- Expose port 8081
- Jalankan aplikasi

---

## 5. REST API Endpoints

| Method | Endpoint | Request Body | Response | Keterangan |
|--------|----------|-------------|----------|------------|
| GET | `/api/produk` | - | `List<Produk>` | Ambil semua produk |
| GET | `/api/produk/{id}` | - | `Produk` atau 404 | Ambil produk by ID |
| POST | `/api/produk` | `{"nama":"...", "satuan":"...", "harga":...}` | `Produk` | Buat produk baru |
| DELETE | `/api/produk/{id}` | - | 204 No Content | Hapus produk |

## 6. Contoh Request/Response

```json
// POST /api/produk
// Request Body:
{
    "nama": "Laptop ASUS",
    "satuan": "unit",
    "harga": 15000000
}

// Response:
{
    "id": 1,
    "nama": "Laptop ASUS",
    "satuan": "unit",
    "harga": 15000000
}
```
