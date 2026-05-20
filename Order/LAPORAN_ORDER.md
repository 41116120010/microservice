# LAPORAN PROJECT 6: ORDER
## Microservice Manajemen Order (CRUD + Inter-Service Communication + RabbitMQ)

---

## 1. Deskripsi Umum

Project **Order** merupakan microservice yang paling kompleks dalam arsitektur ini. Service ini menangani manajemen pesanan (order) dengan fitur:
- CRUD operasi pada data order
- **Inter-service communication** → Berkomunikasi dengan Produk Service untuk mendapatkan detail produk
- **Service Discovery** → Menggunakan Eureka DiscoveryClient untuk menemukan alamat Produk Service
- **Message Queue** → Mengirim notifikasi ke RabbitMQ setiap kali ada perubahan order
- **Auto-calculate** → Otomatis menghitung total harga (harga × jumlah)

---

## 2. Arsitektur & Flow Aplikasi

```
[Client / Gateway]
       |
       | HTTP Request
       v
[OrderController - @RestController]
       |
       v
[OrderService - @Service]
       |
       |--- CRUD ---> [OrderRepository] ---> [H2 Database ~/order]
       |
       |--- Discovery ---> [Eureka Server] ---> [Produk Service]
       |                                              |
       |                                    GET /api/produk/{id}
       |
       |--- Message ---> [RabbitMQ - "myQueue"] ---> [Konsumer Service]
       |                                                    |
       |                                              Email Notification
```

**Flow Utama - Create Order:**
1. Client POST `/api/order` dengan data order
2. OrderController menerima request
3. OrderService menyimpan order ke database (total otomatis dihitung)
4. OrderService mengirim pesan ke RabbitMQ tentang order baru
5. Konsumer Service menerima pesan dan mengirim email notifikasi

**Flow - Get Order with Produk:**
1. Client GET `/api/order/produk/{id}`
2. OrderService mengambil data order dari database
3. OrderService bertanya ke Eureka: "Di mana PRODUK service?"
4. Eureka memberikan alamat Produk Service
5. OrderService memanggil REST API Produk Service untuk mendapatkan detail produk
6. OrderService menggabungkan data order + produk dalam ResponseTemplate
7. Response dikembalikan ke client

---

## 3. Struktur Project

```
Order/
├── src/main/java/com/daffiqtrie/order/
│   ├── OrderApplication.java
│   ├── RabbitMqConfig.java
│   ├── controller/
│   │   └── OrderController.java
│   ├── model/
│   │   └── Order.java
│   ├── repository/
│   │   └── OrderRepository.java
│   ├── service/
│   │   └── OrderService.java
│   └── vo/
│       ├── Produk.java
│       └── ResponseTemplate.java
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
| `spring-boot-h2console` | Web console H2 |
| `spring-boot-starter-data-jpa` | ORM Spring Data JPA |
| `spring-boot-starter-webmvc` | REST API |
| `spring-boot-devtools` | Hot reload |
| `h2` (runtime) | Database H2 |
| `lombok` | Auto-generate boilerplate |
| `spring-cloud-starter-netflix-eureka-client` | Service discovery |
| `spring-boot-starter-amqp` | **RabbitMQ messaging** |

**Spring Cloud Version:** 2025.1.0  
**Spring Boot Version:** 4.0.3

---

### 4.2 application.properties

```properties
spring.application.name=order
server.port=8082
```
**Baris 1-2:** Nama service = "order", port = 8082

```properties
spring.datasource.url=jdbc:h2:~/order;DB_CLOSE_DELAY=-1
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=12345
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
```
**Baris 4-11:** Konfigurasi H2 Database (sama seperti Produk/Pelanggan, file di `~/order`)

```properties
eureka.client.serviceUrl.defaultZone=http://172.18.0.2:8761/eureka/
```
**Baris 12:** Alamat Eureka Server

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```
**Baris 14-17:** Konfigurasi RabbitMQ:
- `host=localhost` → RabbitMQ berjalan di localhost
- `port=5672` → Port default AMQP
- `username/password=guest` → Kredensial default RabbitMQ

---

### 4.3 Order.java (Model/Entity)

```java
package com.daffiqtrie.order.model;
```
**Baris 1:** Package model

```java
import jakarta.persistence.*;
import lombok.Data;
```
**Baris 3-4:** Import JPA annotations dan Lombok

```java
@Entity
@Table(name = "orders")
@Data
public class Order {
```
**Baris 6-9:**
- `@Entity` → Entitas JPA
- `@Table(name = "orders")` → Nama tabel = "orders" (bukan "order" karena "order" adalah reserved keyword SQL)
- `@Data` → Lombok auto-generate getter/setter

```java
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
```
**Baris 11-13:** Primary key auto-increment

```java
    private Integer idProduk;
    private Integer idPelanggan;
    private Long harga;
    private Long jumlah;
    private Long total;
```
**Baris 15-19:** Field order:
- `idProduk` → Foreign key ke Produk (referensi manual, bukan JPA relation)
- `idPelanggan` → Foreign key ke Pelanggan
- `harga` → Harga satuan produk
- `jumlah` → Jumlah item yang dipesan
- `total` → Total harga (harga × jumlah), dihitung otomatis

```java
    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (this.harga != null && this.jumlah != null) {
            this.total = this.harga * this.jumlah;
        }
    }
```
**Baris 21-27:** JPA Lifecycle Callback:
- `@PrePersist` → Dipanggil sebelum INSERT ke database
- `@PreUpdate` → Dipanggil sebelum UPDATE di database
- `calculateTotal()` → Otomatis menghitung `total = harga × jumlah` sebelum data disimpan
- Pengecekan null untuk menghindari NullPointerException

**Variabel/Field:**
| Field | Tipe | Fungsi |
|-------|------|--------|
| `id` | `Integer` | Primary key |
| `idProduk` | `Integer` | Referensi ke ID produk |
| `idPelanggan` | `Integer` | Referensi ke ID pelanggan |
| `harga` | `Long` | Harga satuan |
| `jumlah` | `Long` | Jumlah item |
| `total` | `Long` | Total harga (auto-calculated) |

---

### 4.4 OrderRepository.java

```java
package com.daffiqtrie.order.repository;
```
**Baris 1:** Package repository

```java
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.daffiqtrie.order.model.Order;
```
**Baris 3-6:** Import

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    void deleteByIdPelanggan(Integer idPelanggan);
    List<Order> findByIdPelanggan(Integer idPelanggan);
}
```
**Baris 8-12:**
- `extends JpaRepository<Order, Integer>` → CRUD dasar
- `deleteByIdPelanggan(Integer idPelanggan)` → **Custom query method** - menghapus semua order milik pelanggan tertentu. Spring Data JPA otomatis membuat query: `DELETE FROM orders WHERE id_pelanggan = ?`
- `findByIdPelanggan(Integer idPelanggan)` → **Custom query method** - mencari semua order milik pelanggan tertentu. Query: `SELECT * FROM orders WHERE id_pelanggan = ?`

---

### 4.5 Produk.java (Value Object)

```java
package com.daffiqtrie.order.vo;
```
**Baris 1:** Package `vo` (Value Object) - class yang merepresentasikan data dari service lain

```java
import lombok.Data;
```
**Baris 3:** Import Lombok

```java
@Data
public class Produk {
    private long id;
    private String nama;
    private String satuan;
    private Long harga;
}
```
**Baris 5-10:** Value Object Produk:
- Bukan entity JPA (tidak ada `@Entity`)
- Digunakan untuk menerima response JSON dari Produk Service
- Field sama dengan entity Produk di Produk Service
- `@Data` → Lombok generate getter/setter

---

### 4.6 ResponseTemplate.java (Value Object)

```java
package com.daffiqtrie.order.vo;
```
**Baris 1:** Package vo

```java
import com.daffiqtrie.order.model.Order;
import lombok.Data;
```
**Baris 3-4:** Import model Order dan Lombok

```java
@Data
public class ResponseTemplate {
    private Order order;
    private Produk produk;
}
```
**Baris 6-10:** Template response yang menggabungkan data Order dan Produk:
- `order` → Data order dari database lokal
- `produk` → Data produk dari Produk Service (via REST call)
- Digunakan sebagai response endpoint `/api/order/produk/{id}`

---

### 4.7 RabbitMqConfig.java

```java
package com.daffiqtrie.order;
```
**Baris 1:** Package utama

```java
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
```
**Baris 3-5:** Import:
- `Queue` → Class RabbitMQ yang merepresentasikan antrian pesan
- `@Bean` → Menandai method sebagai bean producer
- `@Configuration` → Menandai class sebagai konfigurasi Spring

```java
@Configuration
public class RabbitMqConfig {
    @Bean
    public Queue myQueue() {
        return new Queue("myQueue", false);
    }
}
```
**Baris 7-12:**
- `@Configuration` → Class ini berisi definisi bean
- `@Bean` → Method ini menghasilkan bean yang dikelola Spring
- `new Queue("myQueue", false)`:
  - Parameter 1: `"myQueue"` → Nama queue di RabbitMQ
  - Parameter 2: `false` → Queue TIDAK durable (hilang saat RabbitMQ restart)

---

### 4.8 OrderService.java

```java
package com.daffiqtrie.order.service;
```
**Baris 1:** Package service

```java
import java.util.List;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Service;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.client.RestTemplate;
import com.daffiqtrie.order.model.Order;
import com.daffiqtrie.order.repository.OrderRepository;
import com.daffiqtrie.order.vo.Produk;
import com.daffiqtrie.order.vo.ResponseTemplate;
```
**Baris 3-14:** Import:
- `Queue`, `RabbitTemplate` → Untuk mengirim pesan ke RabbitMQ
- `ServiceInstance`, `DiscoveryClient` → Untuk service discovery via Eureka
- `RestTemplate` → Untuk HTTP call ke service lain

```java
@Service
public class OrderService {
    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Queue myQueue;
```
**Baris 16-31:** Deklarasi class dan dependency injection:
- `discoveryClient` → Untuk mencari service di Eureka
- `orderRepository` → Akses database order
- `restTemplate` → HTTP client untuk memanggil REST API service lain
- `rabbitTemplate` → Template untuk mengirim pesan ke RabbitMQ
- `myQueue` → Referensi ke queue "myQueue"

```java
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(myQueue.getName(), message);
        System.out.println("Message sent: " + message);
    }
```
**Baris 33-36:** Method `sendMessage()`:
- Mengirim pesan string ke RabbitMQ queue "myQueue"
- `convertAndSend(queueName, message)` → Convert object ke format yang bisa dikirim dan kirim ke queue
- Print log ke console

```java
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
```
**Baris 38-40:** Mengambil semua order

```java
    public Order getOrderById(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }
```
**Baris 42-45:** Mencari order by ID:
- Berbeda dengan Produk/Pelanggan yang return null
- Di sini menggunakan `orElseThrow()` → throw RuntimeException jika tidak ditemukan

```java
    public Order createOrder(Order order) {
        Order savedOrder = orderRepository.save(order);
        sendMessage("Order created: " + "ID: " + savedOrder.getId() + " ID Produk: " + savedOrder.getIdProduk()
                + " Jumlah: " + savedOrder.getJumlah() + " Harga Satuan: " + savedOrder.getHarga() + " Total Harga: "
                + savedOrder.getTotal() + " ID Pelanggan: " + savedOrder.getIdPelanggan());
        return savedOrder;
    }
```
**Baris 47-53:** Method `createOrder()`:
1. Simpan order ke database (total otomatis dihitung oleh `@PrePersist`)
2. Kirim pesan ke RabbitMQ dengan detail order lengkap
3. Return order yang tersimpan

```java
    public Order updateOrder(Order order) {
        sendMessage("Order updated: " + "ID: " + order.getId() + ...);
        return orderRepository.save(order);
    }
```
**Baris 55-59:** Method `updateOrder()`:
1. Kirim pesan notifikasi update ke RabbitMQ
2. Simpan perubahan ke database

```java
    public void deleteOrder(Integer id) {
        sendMessage("Order deleted: " + id);
        orderRepository.deleteById(id);
    }
```
**Baris 61-64:** Method `deleteOrder()`:
1. Kirim pesan notifikasi delete ke RabbitMQ
2. Hapus order dari database

```java
    @org.springframework.transaction.annotation.Transactional
    public void deleteOrdersByPelanggan(Integer idPelanggan) {
        orderRepository.deleteByIdPelanggan(idPelanggan);
    }
```
**Baris 66-69:** Method `deleteOrdersByPelanggan()`:
- `@Transactional` → Operasi ini berjalan dalam satu transaksi database (jika gagal, semua di-rollback)
- Menghapus semua order milik pelanggan tertentu

```java
    public List<Order> getOrdersByPelanggan(Integer idPelanggan) {
        return orderRepository.findByIdPelanggan(idPelanggan);
    }
```
**Baris 71-73:** Mengambil semua order milik pelanggan tertentu

```java
    public ResponseTemplate getOrderWithProduk(Integer id) {
        Order order = getOrderById(id);
        List<ServiceInstance> instances = discoveryClient.getInstances("PRODUK");
        if (instances.isEmpty()) {
            throw new RuntimeException("PRODUK service is not available");
        }
        ServiceInstance serviceInstance = instances.get(0);
        Produk produk = restTemplate.getForObject(
                serviceInstance.getUri() + "/api/produk/" + order.getIdProduk(),
                Produk.class);
        ResponseTemplate vo = new ResponseTemplate();
        vo.setOrder(order);
        vo.setProduk(produk);
        return vo;
    }
```
**Baris 75-89:** Method `getOrderWithProduk()` - **Inter-Service Communication**:
1. Ambil data order dari database lokal
2. Tanya Eureka: "Berikan instance service bernama PRODUK"
3. Jika tidak ada instance → throw exception
4. Ambil instance pertama (index 0)
5. Panggil REST API Produk Service: `GET {uri}/api/produk/{idProduk}`
6. `restTemplate.getForObject()` → HTTP GET dan convert JSON response ke object Produk
7. Gabungkan data order + produk dalam ResponseTemplate
8. Return response gabungan

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `discoveryClient` | `DiscoveryClient` | Mencari service di Eureka |
| `orderRepository` | `OrderRepository` | Akses database |
| `restTemplate` | `RestTemplate` | HTTP client |
| `rabbitTemplate` | `RabbitTemplate` | Kirim pesan RabbitMQ |
| `myQueue` | `Queue` | Referensi queue |
| `instances` | `List<ServiceInstance>` | Daftar instance Produk Service |
| `serviceInstance` | `ServiceInstance` | Instance Produk yang dipilih |

---

### 4.9 OrderController.java

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
```
**Baris 1-5:** REST controller dengan base path `/api/order`

```java
    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }
```
**Endpoint:** GET `/api/order` → Semua order

```java
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(orderService.getOrderById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
```
**Endpoint:** GET `/api/order/{id}` → Order by ID (404 jika tidak ada)

```java
    @GetMapping("/produk/{id}")
    public ResponseEntity<ResponseTemplate> getOrderWithProduk(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(orderService.getOrderWithProduk(id));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not available")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            }
            return ResponseEntity.notFound().build();
        }
    }
```
**Endpoint:** GET `/api/order/produk/{id}` → Order + detail produk:
- 200 OK → Berhasil
- 503 Service Unavailable → Produk Service tidak tersedia
- 404 Not Found → Order tidak ditemukan

```java
    @PostMapping
    public Order createOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }
```
**Endpoint:** POST `/api/order` → Buat order baru

```java
    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable Integer id, @RequestBody Order order) {
        try {
            orderService.getOrderById(id);
            order.setId(id);
            return ResponseEntity.ok(orderService.updateOrder(order));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
```
**Endpoint:** PUT `/api/order/{id}` → Update order:
1. Cek apakah order ada
2. Set ID dari path variable
3. Update dan return

```java
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Integer id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
```
**Endpoint:** DELETE `/api/order/{id}` → Hapus order

```java
    @DeleteMapping("/pelanggan/{idPelanggan}")
    public ResponseEntity<Void> deleteOrdersByPelanggan(@PathVariable Integer idPelanggan) {
        orderService.deleteOrdersByPelanggan(idPelanggan);
        return ResponseEntity.noContent().build();
    }
```
**Endpoint:** DELETE `/api/order/pelanggan/{idPelanggan}` → Hapus semua order pelanggan

```java
    @GetMapping("/pelanggan/{idPelanggan}")
    public List<Order> getOrdersByPelanggan(@PathVariable Integer idPelanggan) {
        return orderService.getOrdersByPelanggan(idPelanggan);
    }
```
**Endpoint:** GET `/api/order/pelanggan/{idPelanggan}` → Ambil semua order pelanggan

---

### 4.10 OrderApplication.java

```java
@SpringBootApplication
@EnableDiscoveryClient
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```
- `@EnableDiscoveryClient` → Daftar ke Eureka
- `@Bean RestTemplate` → Mendefinisikan bean RestTemplate yang bisa di-inject di service lain. RestTemplate digunakan untuk melakukan HTTP call ke microservice lain

---

## 5. REST API Endpoints

| Method | Endpoint | Fungsi |
|--------|----------|--------|
| GET | `/api/order` | Ambil semua order |
| GET | `/api/order/{id}` | Ambil order by ID |
| GET | `/api/order/produk/{id}` | Ambil order + detail produk |
| GET | `/api/order/pelanggan/{idPelanggan}` | Ambil order by pelanggan |
| POST | `/api/order` | Buat order baru |
| PUT | `/api/order/{id}` | Update order |
| DELETE | `/api/order/{id}` | Hapus order |
| DELETE | `/api/order/pelanggan/{idPelanggan}` | Hapus semua order pelanggan |

## 6. Contoh Request/Response

```json
// POST /api/order
{
    "idProduk": 1,
    "idPelanggan": 1,
    "harga": 15000000,
    "jumlah": 2
}

// Response (total otomatis dihitung):
{
    "id": 1,
    "idProduk": 1,
    "idPelanggan": 1,
    "harga": 15000000,
    "jumlah": 2,
    "total": 30000000
}
```
