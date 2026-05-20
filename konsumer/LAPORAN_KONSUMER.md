# LAPORAN PROJECT 8: KONSUMER
## Message Consumer dengan RabbitMQ + Email Notification

---

## 1. Deskripsi Umum

Project **konsumer** merupakan microservice yang berfungsi sebagai **message consumer** (penerima pesan) dari RabbitMQ. Service ini mendengarkan pesan dari queue "myQueue" dan ketika menerima pesan order baru, secara otomatis mengirim email notifikasi ke alamat yang dikonfigurasi. Ini merupakan implementasi **event-driven architecture** di mana aksi (pengiriman email) dipicu oleh event (pesan masuk di queue).

**Fungsi Utama:**
- Mendengarkan (listen) pesan dari RabbitMQ queue "myQueue"
- Memproses pesan yang berisi informasi order baru
- Mengirim email notifikasi ke penerima yang dikonfigurasi
- Demonstrasi konsep Consumer dalam pola messaging dan integrasi email (SMTP)

---

## 2. Arsitektur & Flow Aplikasi

```
[Order Service / Produser Service]
       |
       | Mengirim pesan ke queue
       v
[RabbitMQ Server - Port 5672]
       |
       | Queue: "myQueue"
       v
[KonsumerService - @RabbitListener]
       |
       | Menerima pesan
       | Memproses pesan "Order created:..."
       v
[EmailService - @Service]
       |
       | Mengirim email via SMTP
       v
[Gmail SMTP Server - smtp.gmail.com:587]
       |
       v
[Email diterima oleh: raemon@pnp.ac.id]
```

**Flow Detail:**
1. Order Service membuat order baru dan mengirim pesan ke RabbitMQ
2. Pesan masuk ke queue "myQueue"
3. KonsumerService yang memiliki `@RabbitListener` otomatis menerima pesan
4. KonsumerService memeriksa apakah pesan dimulai dengan "Order created:"
5. Jika ya, KonsumerService mengekstrak detail order dari pesan
6. KonsumerService menyusun body email yang rapi
7. EmailService mengirim email melalui Gmail SMTP
8. Email notifikasi diterima oleh dosen (raemon@pnp.ac.id)

---

## 3. Struktur Project

```
konsumer/
├── src/main/java/com/daffiqtrie/konsumer/
│   ├── KonsumerApplication.java
│   ├── KonsumerService.java
│   ├── EmailService.java
│   └── RabbitMqConfig.java
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── dockerfile (kosong)
```

---

## 4. Analisa File per File

### 4.1 pom.xml

| Dependency | Fungsi |
|-----------|--------|
| `spring-boot-starter-amqp` | Integrasi RabbitMQ (consumer) |
| `spring-boot-starter-mail` | **Spring Mail** - untuk mengirim email via SMTP |
| `spring-boot-starter-amqp-test` | Testing AMQP |

**Spring Boot Version:** 4.0.5  
**Java Version:** 17

**Catatan:** Project ini TIDAK memiliki `spring-boot-starter-webmvc` karena tidak menyediakan REST API. Service ini hanya mendengarkan pesan dari RabbitMQ.

---

### 4.2 application.properties

```properties
spring.application.name=konsumer
```
**Baris 1:** Nama aplikasi = "konsumer"

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```
**Baris 2-5:** Konfigurasi RabbitMQ:
- Koneksi ke RabbitMQ di localhost port 5672
- Menggunakan kredensial default

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=daffiqcoder@gmail.com
spring.mail.password=dvzf gaou phwr ytig
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```
**Baris 7-12:** Konfigurasi SMTP Email:
- `host=smtp.gmail.com` → Menggunakan Gmail SMTP server
- `port=587` → Port SMTP dengan STARTTLS
- `username` → Alamat email pengirim
- `password` → App Password Gmail (bukan password akun biasa, ini adalah password khusus aplikasi yang di-generate dari Google Account)
- `smtp.auth=true` → Mengaktifkan autentikasi SMTP
- `smtp.starttls.enable=true` → Mengaktifkan enkripsi TLS untuk keamanan

```properties
app.mail.to=raemon@pnp.ac.id
```
**Baris 14:** Custom property - alamat email tujuan notifikasi (email dosen)

---

### 4.3 RabbitMqConfig.java

```java
package com.daffiqtrie.konsumer;
```
**Baris 1:** Package

```java
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
```
**Baris 3-5:** Import Queue, Bean, dan Configuration

```java
@Configuration
public class RabbitMqConfig {
    @Bean
    public Queue myQueue() {
        return new Queue("myQueue", false);
    }
}
```
**Baris 7-12:** Konfigurasi queue:
- Mendefinisikan queue "myQueue" yang sama dengan yang digunakan oleh Produser/Order Service
- `durable = false` → Queue tidak persisten
- Queue ini harus sama namanya dengan queue di producer agar pesan bisa diterima

---

### 4.4 EmailService.java

```java
package com.daffiqtrie.konsumer;
```
**Baris 1:** Package

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
```
**Baris 3-6:** Import:
- `@Autowired` → Dependency injection
- `SimpleMailMessage` → Class untuk membuat email teks biasa (tanpa HTML/attachment)
- `JavaMailSender` → Interface Spring untuk mengirim email. Auto-configured oleh Spring Boot berdasarkan properties `spring.mail.*`
- `@Service` → Menandai sebagai service bean

```java
@Service
public class EmailService {
```
**Baris 8-9:** Class EmailService ditandai sebagai service

```java
    @Autowired
    private JavaMailSender mailSender;
```
**Baris 11-12:**
- `JavaMailSender mailSender` → Bean yang otomatis dikonfigurasi oleh Spring Boot menggunakan properties SMTP di application.properties
- Menyediakan method `send()` untuk mengirim email

```java
    public void sendOrderNotification(String to, String subject, String body) {
```
**Baris 14:** Method untuk mengirim email notifikasi:
- `to` → Alamat email tujuan
- `subject` → Judul email
- `body` → Isi email

```java
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
```
**Baris 15-18:** Membuat dan mengisi email:
- `new SimpleMailMessage()` → Membuat object email baru
- `setTo(to)` → Set alamat tujuan
- `setSubject(subject)` → Set judul email
- `setText(body)` → Set isi email (plain text)

```java
        mailSender.send(message);
        System.out.println("Email notifikasi berhasil dikirim ke: " + to);
    }
```
**Baris 20-21:** Mengirim email:
- `mailSender.send(message)` → Mengirim email melalui SMTP server yang dikonfigurasi
- Print log konfirmasi ke console

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `mailSender` | `JavaMailSender` | SMTP client untuk kirim email |
| `message` | `SimpleMailMessage` | Object email yang akan dikirim |
| `to` | `String` | Alamat email tujuan |
| `subject` | `String` | Judul email |
| `body` | `String` | Isi email |

---

### 4.5 KonsumerService.java

```java
package com.daffiqtrie.konsumer;
```
**Baris 1:** Package

```java
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
```
**Baris 3-6:** Import:
- `@RabbitListener` → Annotation yang menandai method sebagai listener queue RabbitMQ
- `@Autowired` → Dependency injection
- `@Value` → Mengambil nilai dari application.properties
- `@Service` → Service bean

```java
@Service
public class KonsumerService {
```
**Baris 8-9:** Class service

```java
    @Autowired
    private EmailService emailService;
```
**Baris 11-12:** Inject EmailService untuk mengirim email

```java
    @Value("${app.mail.to}")
    private String emailTujuan;
```
**Baris 14-15:**
- `@Value("${app.mail.to}")` → Mengambil nilai property `app.mail.to` dari application.properties
- `emailTujuan` = "raemon@pnp.ac.id" (alamat email dosen)

```java
    @RabbitListener(queues = "myQueue")
    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);
```
**Baris 17-19:** Method listener:
- `@RabbitListener(queues = "myQueue")` → Method ini otomatis dipanggil setiap kali ada pesan baru di queue "myQueue"
- `String message` → Pesan yang diterima dari queue
- Print log pesan yang diterima

```java
        if (message.startsWith("Order created:")) {
            String subject = "Notifikasi Order Baru";
            String body = buildEmailBody(message);
            emailService.sendOrderNotification(emailTujuan, subject, body);
        }
    }
```
**Baris 21-26:** Logika pemrosesan:
- Hanya memproses pesan yang dimulai dengan "Order created:" (filter pesan)
- Menyusun subject email
- Memanggil `buildEmailBody()` untuk membuat isi email yang rapi
- Mengirim email notifikasi

```java
    private String extractValue(String message, String key) {
        int startIndex = message.indexOf(key);
        if (startIndex == -1) {
            return null;
        }
        startIndex += key.length();
        String remaining = message.substring(startIndex).trim();
        int endIndex = remaining.indexOf(" ");
        if (endIndex == -1) {
            return remaining;
        }
        return remaining.substring(0, endIndex);
    }
```
**Baris 28-40:** Method `extractValue()` - Utility untuk parsing pesan:
- `message.indexOf(key)` → Mencari posisi key dalam pesan
- Jika key tidak ditemukan → return null
- `startIndex += key.length()` → Pindah posisi ke setelah key
- `message.substring(startIndex).trim()` → Ambil sisa string setelah key, hapus spasi
- `remaining.indexOf(" ")` → Cari spasi berikutnya (akhir nilai)
- Jika tidak ada spasi → return sisa string (nilai terakhir)
- Jika ada spasi → return substring dari awal sampai spasi

**Contoh:**
```
message = "Order created: ID: 1 ID Produk: 2 Jumlah: 3"
extractValue(message, "ID:") → "1"
extractValue(message, "ID Produk:") → "2"
extractValue(message, "Jumlah:") → "3"
```

```java
    private String buildEmailBody(String message) {
        String orderId = extractValue(message, "ID:");
        String idProduk = extractValue(message, "ID Produk:");
        String jumlah = extractValue(message, "Jumlah:");
        String hargaSatuan = extractValue(message, "Harga Satuan:");
        String totalHarga = extractValue(message, "Total Harga:");
        String idPelanggan = extractValue(message, "ID Pelanggan:");
```
**Baris 42-48:** Mengekstrak semua detail order dari pesan RabbitMQ

```java
        StringBuilder body = new StringBuilder();
        body.append("Halo,\n\n");
        body.append("Terima kasih telah melakukan pemesanan. Berikut adalah detail order Anda:\n\n");
        body.append("==============================\n");
        body.append("  DETAIL ORDER\n");
        body.append("==============================\n");
        body.append("  Order ID       : ").append(orderId).append("\n");
        body.append("  ID Produk      : ").append(idProduk).append("\n");
        body.append("  Jumlah         : ").append(jumlah).append("\n");
        body.append("  Harga Satuan   : Rp ").append(hargaSatuan).append("\n");
        body.append("  Total Harga    : Rp ").append(totalHarga).append("\n");
        body.append("  ID Pelanggan   : ").append(idPelanggan).append("\n");
        body.append("==============================\n\n");
        body.append("Pesanan Anda sedang diproses.\n\n");
        body.append("Salam,\n");
        body.append("Daffiq Trie Octorino");
        return body.toString();
    }
```
**Baris 50-67:** Menyusun body email:
- Menggunakan `StringBuilder` untuk efisiensi string concatenation
- Format email yang rapi dengan header, detail order, dan footer
- Menampilkan semua informasi order dalam format yang mudah dibaca

**Variabel:**
| Variabel | Tipe | Fungsi |
|----------|------|--------|
| `emailService` | `EmailService` | Service untuk kirim email |
| `emailTujuan` | `String` | Alamat email tujuan (dari properties) |
| `message` | `String` | Pesan dari RabbitMQ |
| `orderId` | `String` | ID order yang diekstrak |
| `idProduk` | `String` | ID produk yang diekstrak |
| `jumlah` | `String` | Jumlah item |
| `hargaSatuan` | `String` | Harga per unit |
| `totalHarga` | `String` | Total harga |
| `idPelanggan` | `String` | ID pelanggan |
| `body` | `StringBuilder` | Builder untuk isi email |
| `startIndex` | `int` | Posisi awal key dalam string |
| `remaining` | `String` | Sisa string setelah key |
| `endIndex` | `int` | Posisi spasi berikutnya |

---

### 4.6 KonsumerApplication.java

```java
package com.daffiqtrie.konsumer;
```
**Baris 1:** Package

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
```
**Baris 3-4:** Import Spring Boot

```java
@SpringBootApplication
public class KonsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(KonsumerApplication.class, args);
    }
}
```
**Baris 6-11:**
- `@SpringBootApplication` → Auto-configuration
- Saat startup, Spring Boot otomatis:
  1. Connect ke RabbitMQ
  2. Mendaftarkan listener pada queue "myQueue"
  3. Siap menerima pesan

---

## 5. Contoh Email yang Dikirim

```
To: raemon@pnp.ac.id
Subject: Notifikasi Order Baru

Halo,

Terima kasih telah melakukan pemesanan. Berikut adalah detail order Anda:

==============================
  DETAIL ORDER
==============================
  Order ID       : 1
  ID Produk      : 2
  Jumlah         : 3
  Harga Satuan   : Rp 15000000
  Total Harga    : Rp 45000000
  ID Pelanggan   : 1
==============================

Pesanan Anda sedang diproses.

Salam,
Daffiq Trie Octorino
```

## 6. Hubungan dengan Project Lain

- **Menerima pesan dari:** Order Service dan Produser Service (via RabbitMQ)
- **Mengirim email ke:** raemon@pnp.ac.id (dosen)
- **Tidak bergantung pada:** Eureka (standalone consumer)
- **Trigger:** Setiap kali Order Service membuat order baru

## 7. Catatan Teknis

- Service ini **tidak memiliki REST API** (tidak ada web endpoint)
- Service berjalan sebagai background listener
- Menggunakan **App Password** Gmail (bukan password biasa) untuk autentikasi SMTP
- Hanya memproses pesan yang dimulai dengan "Order created:" (pesan update/delete diabaikan)
