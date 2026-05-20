# LAPORAN PROJECT 7: PRODUSER
## Message Producer dengan RabbitMQ

---

## 1. Deskripsi Umum

Project **produser** merupakan microservice yang berfungsi sebagai **message producer** (pengirim pesan) menggunakan RabbitMQ. Service ini menyediakan REST API endpoint untuk mengirim pesan ke message queue "myQueue". Pesan yang dikirim akan dikonsumsi oleh Konsumer Service.

**Fungsi Utama:**
- Menerima pesan dari client melalui REST API
- Mengirim pesan ke RabbitMQ queue "myQueue"
- Demonstrasi konsep Producer dalam pola messaging (Producer-Consumer Pattern)

---

## 2. Arsitektur & Flow Aplikasi

```
[Client/Browser]
       |
       | GET /send?message=Hello
       v
[ProduserController - @RestController]
       |
       | Memanggil sendMessage()
       v
[ProduserService - @Service]
       |
       | rabbitTemplate.convertAndSend()
       v
[RabbitMQ Server - Port 5672]
       |
       | Queue: "myQueue"
       v
[Konsumer Service - @RabbitListener]
       |
       v
[Proses pesan / Email notification]
```

**Flow:**
1. Client mengirim GET request ke `/send?message=Hello`
2. ProduserController menerima request dan memanggil ProduserService
3. ProduserService menggunakan RabbitTemplate untuk mengirim pesan ke queue "myQueue"
4. RabbitMQ menyimpan pesan di queue
5. Konsumer Service yang listen pada queue "myQueue" menerima dan memproses pesan

---

## 3. Struktur Project

```
produser/
├── src/main/java/com/daffiqtrie/produser/
│   ├── ProduserApplication.java
│   ├── ProduserController.java
│   ├── ProduserService.java
│   └── RabbitMqConfig.java
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

---

## 4. Analisa File per File

### 4.1 pom.xml

| Dependency | Fungsi |
|-----------|--------|
| `spring-boot-starter-amqp` | **Dependency utama** - Spring AMQP untuk integrasi RabbitMQ |
| `spring-boot-starter-webmvc` | REST API framework |
| `spring-boot-starter-amqp-test` | Testing untuk AMQP |
| `spring-boot-starter-webmvc-test` | Testing untuk web |

**Spring Boot Version:** 4.0.5  
**Java Version:** 17

**Catatan:** Project ini TIDAK menggunakan Eureka Client (standalone messaging service)

---

### 4.2 application.properties

```properties
spring.application.name=produser
```
**Baris 1:** Nama aplikasi = "produser"

```properties
server.port=8080
```
**Baris 2:** Port aplikasi = **8080** (port default Spring Boot)

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```
**Baris 3-6:** Konfigurasi koneksi RabbitMQ:
- `host=localhost` → RabbitMQ berjalan di mesin yang sama
- `port=5672` → Port default AMQP protocol
- `username=guest` → Username default RabbitMQ
- `password=guest` → Password default RabbitMQ

---

### 4.3 RabbitMqConfig.java

```java
package com.daffiqtrie.produser;
```
**Baris 1:** Package utama

```java
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
```
**Baris 3-5:** Import:
- `Queue` → Class dari Spring AMQP yang merepresentasikan RabbitMQ queue
- `@Bean` → Menandai method sebagai bean factory
- `@Configuration` → Menandai class sebagai konfigurasi Spring

```java
@Configuration
public class RabbitMqConfig {
```
**Baris 7-8:**
- `@Configuration` → Spring memproses class ini saat startup untuk membuat bean

```java
    @Bean
    public Queue myQueue() {
        return new Queue("myQueue", false);
    }
```
**Baris 9-12:** Mendefinisikan bean Queue:
- `@Bean` → Method ini menghasilkan bean yang dikelola Spring container
- `new Queue("myQueue", false)`:
  - `"myQueue"` → Nama queue di RabbitMQ broker
  - `false` → `durable = false` → Queue TIDAK persisten (hilang saat RabbitMQ restart)
  - Jika queue belum ada di RabbitMQ, akan otomatis dibuat saat aplikasi connect

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| Return value | `Queue` | Object queue yang merepresentasikan "myQueue" di RabbitMQ |

---

### 4.4 ProduserService.java

```java
package com.daffiqtrie.produser;
```
**Baris 1:** Package

```java
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
```
**Baris 3-6:** Import:
- `Queue` → Referensi ke queue yang sudah didefinisikan di config
- `RabbitTemplate` → Template class untuk operasi RabbitMQ (kirim/terima pesan)
- `@Autowired` → Dependency injection
- `@Service` → Menandai sebagai service bean

```java
@Service
public class ProduserService {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private Queue myQueue;
```
**Baris 8-14:**
- `@Service` → Spring mengelola class ini sebagai service bean
- `rabbitTemplate` → Template untuk mengirim pesan ke RabbitMQ. Spring Boot auto-configure berdasarkan properties `spring.rabbitmq.*`
- `myQueue` → Bean Queue yang didefinisikan di RabbitMqConfig (nama bean = nama method = "myQueue")

```java
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(myQueue.getName(), message);
        System.out.println("Message sent: " + message);
    }
```
**Baris 16-19:** Method `sendMessage()`:
- Parameter: `String message` → Pesan yang akan dikirim
- `rabbitTemplate.convertAndSend(queueName, message)`:
  - Parameter 1: `myQueue.getName()` → Nama queue tujuan = "myQueue"
  - Parameter 2: `message` → Pesan yang dikirim
  - Method ini mengkonversi object ke format byte dan mengirim ke RabbitMQ
- `System.out.println(...)` → Log ke console untuk debugging

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `rabbitTemplate` | `RabbitTemplate` | Client untuk operasi RabbitMQ |
| `myQueue` | `Queue` | Referensi queue tujuan |
| `message` | `String` | Pesan yang akan dikirim |

---

### 4.5 ProduserController.java

```java
package com.daffiqtrie.produser;
```
**Baris 1:** Package

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
```
**Baris 3-6:** Import:
- `@Autowired` → Dependency injection
- `@GetMapping` → Mapping HTTP GET
- `@RequestParam` → Mengambil query parameter
- `@RestController` → REST controller

```java
@RestController
public class ProduserController {
    @Autowired
    private ProduserService produserService;
```
**Baris 8-11:**
- `@RestController` → Class ini menangani HTTP request dan return data langsung
- `produserService` → Inject service untuk mengirim pesan

```java
    @GetMapping("/send")
    public String sendMessage(@RequestParam String message) {
        produserService.sendMessage(message);
        return "Message sent: " + message;
    }
```
**Baris 13-17:** Endpoint GET `/send`:
- `@GetMapping("/send")` → Memetakan GET request ke path `/send`
- `@RequestParam String message` → Mengambil parameter `message` dari query string (wajib)
- Memanggil service untuk mengirim pesan ke RabbitMQ
- Return konfirmasi ke client: "Message sent: {message}"

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `produserService` | `ProduserService` | Service untuk kirim pesan |
| `message` | `String` | Pesan dari query parameter |

---

### 4.6 ProduserApplication.java

```java
package com.daffiqtrie.produser;
```
**Baris 1:** Package

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
```
**Baris 3-4:** Import Spring Boot

```java
@SpringBootApplication
public class ProduserApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProduserApplication.class, args);
    }
}
```
**Baris 6-11:**
- `@SpringBootApplication` → Auto-configuration, component scan
- `main()` → Entry point, menjalankan Spring Boot application

---

## 5. REST API Endpoints

| Method | Endpoint | Parameter | Response |
|--------|----------|-----------|----------|
| GET | `/send` | `message` (required) | "Message sent: {message}" |

## 6. Contoh Penggunaan

```
GET http://localhost:8080/send?message=Hello%20World
Response: "Message sent: Hello World"

# Pesan "Hello World" dikirim ke RabbitMQ queue "myQueue"
# Konsumer Service akan menerima pesan ini
```

## 7. Hubungan dengan Project Lain

- **Mengirim pesan ke:** RabbitMQ queue "myQueue"
- **Pesan dikonsumsi oleh:** Konsumer Service
- **Tidak bergantung pada:** Eureka (standalone)
