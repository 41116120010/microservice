# LAPORAN PROJECT 5: PELANGGAN
## Microservice Manajemen Pelanggan (CRUD + Eureka Client)

---

## 1. Deskripsi Umum

Project **Pelanggan** merupakan microservice yang menangani manajemen data pelanggan. Service ini menyediakan REST API untuk operasi CRUD pada entitas Pelanggan. Strukturnya sangat mirip dengan Produk Service, menggunakan H2 Database dan terdaftar di Eureka Server.

**Fungsi Utama:**
- Menyimpan dan mengelola data pelanggan (nama, alamat)
- Menyediakan REST API CRUD
- Terdaftar di Eureka untuk service discovery

---

## 2. Arsitektur & Flow Aplikasi

```
[Client / Gateway]
       |
       | HTTP Request
       v
[PelangganController - @RestController]
       |
       v
[PelangganService - @Service]
       |
       v
[PelangganRepository - JpaRepository]
       |
       v
[H2 Database - file ~/pelanggan]
```

**Flow CRUD:**
1. Client mengirim request ke `/api/pelanggan`
2. Controller menerima dan meneruskan ke Service
3. Service memanggil Repository
4. Repository mengeksekusi query ke H2 Database
5. Response dikembalikan ke client

---

## 3. Struktur Project

```
Pelanggan/
├── src/main/java/com/daffiqtrie/pelanggan/
│   ├── PelangganApplication.java
│   ├── controller/
│   │   └── PelangganController.java
│   ├── model/
│   │   └── Pelanggan.java
│   ├── repository/
│   │   └── PelangganRepository.java
│   └── service/
│       └── PelangganService.java
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
| `spring-boot-h2console` | Web console H2 database |
| `spring-boot-starter-data-jpa` | ORM dengan Spring Data JPA |
| `spring-boot-starter-webmvc` | REST API framework |
| `spring-boot-devtools` | Hot reload development |
| `h2` (runtime) | Database H2 embedded |
| `lombok` | Auto-generate getter/setter |
| `spring-cloud-starter-netflix-eureka-client` | Registrasi ke Eureka |

**Spring Cloud Version:** 2025.1.0  
**Spring Boot Version:** 4.0.3  
**Java Version:** 17

---

### 4.2 application.properties

```properties
spring.application.name=pelanggan
```
**Baris 1:** Nama service di Eureka = "pelanggan"

```properties
server.port=8083
```
**Baris 2:** Port aplikasi = **8083**

```properties
spring.datasource.url=jdbc:h2:~/pelanggan;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=12345
```
**Baris 4-7:** Konfigurasi database H2:
- File database di `~/pelanggan`
- Driver H2
- Username: sa, Password: 12345

```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```
**Baris 8-9:** H2 Console aktif di `/h2-console`

```properties
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
```
**Baris 10-11:** JPA/Hibernate konfigurasi - auto create/update tabel

```properties
eureka.client.serviceUrl.defaultZone=http://172.18.0.2:8761/eureka/
```
**Baris 12:** Alamat Eureka Server

---

### 4.3 Pelanggan.java (Model/Entity)

```java
package com.daffiqtrie.pelanggan.model;
```
**Baris 1:** Package model

```java
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
```
**Baris 3-7:** Import JPA annotations dan Lombok

```java
@Entity
@Data
public class Pelanggan {
```
**Baris 9-11:**
- `@Entity` → Dipetakan ke tabel "pelanggan" di database
- `@Data` → Lombok generate getter, setter, toString, equals, hashCode

```java
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String nama;
    private String alamat;
```
**Baris 12-16:** Field entitas:
- `id` → Primary key, auto-increment, tipe Integer
- `nama` → Nama pelanggan
- `alamat` → Alamat pelanggan

**Variabel/Field:**
| Field | Tipe | Fungsi | Kolom DB |
|-------|------|--------|----------|
| `id` | `Integer` | Primary key, auto-increment | `id` |
| `nama` | `String` | Nama pelanggan | `nama` |
| `alamat` | `String` | Alamat pelanggan | `alamat` |

---

### 4.4 PelangganRepository.java

```java
package com.daffiqtrie.pelanggan.repository;
```
**Baris 1:** Package repository

```java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.daffiqtrie.pelanggan.model.Pelanggan;
```
**Baris 3-5:** Import JpaRepository, annotation Repository, dan model Pelanggan

```java
@Repository
public interface PelangganRepository extends JpaRepository<Pelanggan, Integer> {
}
```
**Baris 7-9:**
- `extends JpaRepository<Pelanggan, Integer>` → Mewarisi method CRUD:
  - `findAll()` → Ambil semua pelanggan
  - `findById(Integer id)` → Cari by ID
  - `save(Pelanggan)` → Simpan/update
  - `deleteById(Integer id)` → Hapus by ID
- Generic: Entity = `Pelanggan`, Primary Key = `Integer`

---

### 4.5 PelangganService.java

```java
package com.daffiqtrie.pelanggan.service;
```
**Baris 1:** Package service

```java
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.daffiqtrie.pelanggan.model.Pelanggan;
import com.daffiqtrie.pelanggan.repository.PelangganRepository;
```
**Baris 3-7:** Import yang diperlukan

```java
@Service
public class PelangganService {
    @Autowired
    private PelangganRepository pelangganRepository;
```
**Baris 9-12:**
- `@Service` → Menandai sebagai service bean
- `@Autowired` → Inject repository

```java
    public List<Pelanggan> getAllPelanggan() {
        return pelangganRepository.findAll();
    }
```
**Baris 14-16:** Mengambil semua data pelanggan dari database

```java
    public Pelanggan getPelangganById(Integer id) {
        return pelangganRepository.findById(id).orElse(null);
    }
```
**Baris 18-20:** Mencari pelanggan by ID, return null jika tidak ada

```java
    public Pelanggan createPelanggan(Pelanggan pelanggan) {
        return pelangganRepository.save(pelanggan);
    }
```
**Baris 22-24:** Menyimpan pelanggan baru ke database

```java
    public void deletePelanggan(Integer id) {
        pelangganRepository.deleteById(id);
    }
```
**Baris 26-28:** Menghapus pelanggan berdasarkan ID

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `pelangganRepository` | `PelangganRepository` | Akses database pelanggan |

---

### 4.6 PelangganController.java

```java
package com.daffiqtrie.pelanggan.controller;
```
**Baris 1:** Package controller

```java
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.daffiqtrie.pelanggan.model.Pelanggan;
import com.daffiqtrie.pelanggan.service.PelangganService;
```
**Baris 3-8:** Import REST annotations, ResponseEntity, model, dan service

```java
@RestController
@RequestMapping("/api/pelanggan")
public class PelangganController {
    @Autowired
    private PelangganService pelangganService;
```
**Baris 10-14:**
- `@RestController` → REST controller (JSON response)
- `@RequestMapping("/api/pelanggan")` → Base path
- Inject PelangganService

```java
    @GetMapping
    public List<Pelanggan> getAllPelanggan() {
        return pelangganService.getAllPelanggan();
    }
```
**Baris 16-19:** GET `/api/pelanggan` → Ambil semua pelanggan

```java
    @GetMapping("/{id}")
    public ResponseEntity<Pelanggan> getPelangganById(@PathVariable Integer id) {
        Pelanggan pelanggan = pelangganService.getPelangganById(id);
        return pelanggan != null ? ResponseEntity.ok(pelanggan) : ResponseEntity.notFound().build();
    }
```
**Baris 21-25:** GET `/api/pelanggan/{id}`:
- Cari pelanggan by ID
- Return 200 + data jika ditemukan
- Return 404 jika tidak ditemukan

```java
    @PostMapping
    public Pelanggan createPelanggan(@RequestBody Pelanggan pelanggan) {
        return pelangganService.createPelanggan(pelanggan);
    }
```
**Baris 27-30:** POST `/api/pelanggan` → Buat pelanggan baru dari JSON body

```java
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePelanggan(@PathVariable Integer id) {
        pelangganService.deletePelanggan(id);
        return ResponseEntity.noContent().build();
    }
```
**Baris 32-36:** DELETE `/api/pelanggan/{id}` → Hapus pelanggan, return 204

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `pelangganService` | `PelangganService` | Referensi service layer |
| `id` | `Integer` | ID pelanggan dari URL |
| `pelanggan` | `Pelanggan` | Object pelanggan dari request/response |

---

### 4.7 PelangganApplication.java

```java
@SpringBootApplication
@EnableDiscoveryClient
public class PelangganApplication {
    public static void main(String[] args) {
        SpringApplication.run(PelangganApplication.class, args);
    }
}
```
- `@SpringBootApplication` → Auto-configuration
- `@EnableDiscoveryClient` → Mendaftar ke Eureka Server
- `main()` → Entry point

---

### 4.8 dockerfile

```dockerfile
FROM openjdk:27-ea-17-jdk-slim-trixie
WORKDIR /app
COPY target/pelanggan-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8083
CMD ["java", "-jar", "app.jar"]
```
- Expose port 8083
- Menggunakan `CMD` (bukan ENTRYPOINT) → bisa di-override saat docker run

---

## 5. REST API Endpoints

| Method | Endpoint | Request Body | Response | Keterangan |
|--------|----------|-------------|----------|------------|
| GET | `/api/pelanggan` | - | `List<Pelanggan>` | Ambil semua pelanggan |
| GET | `/api/pelanggan/{id}` | - | `Pelanggan` atau 404 | Ambil pelanggan by ID |
| POST | `/api/pelanggan` | `{"nama":"...", "alamat":"..."}` | `Pelanggan` | Buat pelanggan baru |
| DELETE | `/api/pelanggan/{id}` | - | 204 No Content | Hapus pelanggan |

## 6. Contoh Request/Response

```json
// POST /api/pelanggan
// Request Body:
{
    "nama": "Daffiq Trie",
    "alamat": "Padang, Sumatera Barat"
}

// Response:
{
    "id": 1,
    "nama": "Daffiq Trie",
    "alamat": "Padang, Sumatera Barat"
}
```
