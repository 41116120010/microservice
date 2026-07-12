# Laporan Analisis Microservice Learning

## 1. Tujuan dan batasan

Repositori ini adalah proyek pembelajaran mata kuliah Pemrograman Microservice. Fokusnya adalah memahami hubungan antar-service, service discovery, autentikasi JWT, komunikasi asynchronous menggunakan RabbitMQ, serta monitoring metric dan log.

Stack ini ditujukan untuk **local development pada Linux Mint** dengan Docker/Portainer, bukan production internet-facing. Karena laptop yang digunakan memiliki RAM 8 GB dan sekitar 5 GB tersedia ketika idle, Compose sudah diberi batas memory agar satu service tidak mengambil seluruh resource host.

Service yang dianalisis:

- `auth-service`
- `eureka`
- `gateway`
- `Produk`
- `Pelanggan`
- `Order`
- `produser`
- `konsumer` (opsional melalui profile `notifications`)
- `rabbitmq`
- Prometheus, Grafana, Elasticsearch, Logstash, dan Kibana

---

## 2. Gambaran arsitektur

```text
Client / Postman
       |
       v
Gateway :9310
       |
       +--> Auth Service :9300       (login + JWKS)
       +--> Produk :8081             (catalog produk)
       +--> Pelanggan :8083          (data pelanggan)
       +--> Order :8082              (order + integrasi Produk)
       +--> Produser :8080           (publish pesan)
                                      |
                                      v
                                  RabbitMQ
                                      |
                                      v
                         Konsumer :8084 (opsional, email)

Semua service Spring Boot --> Eureka :8761 (service discovery)
Semua metric --> Prometheus :9090 --> Grafana :3000
Semua log JSON --> Logstash --> Elasticsearch --> Kibana :5601
RabbitMQ metric --> Prometheus
```

### Port penting

| Komponen | Port Docker/internal | Port host | Keterangan |
|---|---:|---:|---|
| Eureka | 8761 | 8761 | Dashboard dan registry service discovery |
| Auth Service | 9300 | tidak dipublish | Sengaja tidak memakai port 9000; login melalui Gateway |
| Gateway | 9310 | 9310 | Satu pintu API dari host |
| Produk | 8081 | tidak dipublish | Diakses melalui Gateway atau network Docker |
| Order | 8082 | tidak dipublish | Diakses melalui Gateway |
| Pelanggan | 8083 | tidak dipublish | Diakses melalui Gateway |
| Produser | 8080 | tidak dipublish | Route Gateway `/send` |
| Konsumer | 8084 | tidak dipublish | Aktif hanya profile `notifications` |
| RabbitMQ management | 15672 | 15672 | UI management RabbitMQ |
| RabbitMQ AMQP | 5672 | tidak dipublish | Komunikasi internal antar-container |
| RabbitMQ Prometheus | 15692 | tidak dipublish | Scrape internal Prometheus |
| Prometheus | 9090 | 9090 | UI dan query metric |
| Grafana | 3000 | 3000 | Dashboard metric |
| Kibana | 5601 | 5601 | Pencarian dan visualisasi log |
| Logstash | 5000 | tidak dipublish | Input TCP log internal |

Port `9000` tidak digunakan oleh Auth Service. Port internal Auth adalah `9300`, dan semua URL issuer/JWKS/Prometheus sudah diselaraskan ke port tersebut.

---

## 3. Alur kerja utama

### 3.1 Startup

1. Docker Compose membuat network internal dan volume monitoring.
2. `rabbitmq` hidup dan melakukan health check `rabbitmq-diagnostics ping`.
3. `eureka` berjalan sebagai registry tunggal; Eureka tidak mendaftarkan dirinya sendiri.
4. Service Spring Boot membaca `EUREKA_URL=http://eureka:8761/eureka/`.
5. Service aplikasi mendaftarkan nama service ke Eureka.
6. Resource server menggunakan `AUTH_JWK_SET_URI=http://auth-service:9300/.well-known/jwks.json` untuk memvalidasi JWT.
7. Prometheus melakukan scrape ke endpoint `/actuator/prometheus`.
8. Logback mengirim log JSON secara asynchronous ke `logstash:5000`.

Nama seperti `eureka`, `auth-service`, `rabbitmq`, dan `logstash` adalah DNS service dari network Compose. Jangan mengganti nama tersebut dengan `localhost` di dalam container. `localhost` di dalam container berarti container itu sendiri.

### 3.2 Login dan JWT

1. Client mengirim `POST http://localhost:9310/auth/token`.
2. Gateway meneruskan route `/auth/**` ke `AUTH-SERVICE` melalui Eureka.
3. Auth Service mencari username di tabel `app_users`.
4. Password dibandingkan dengan hash BCrypt, bukan dibandingkan sebagai plaintext.
5. Jika benar, Auth Service membuat JWT RSA dengan claim utama:
   - `iss`: `http://auth-service:9300`
   - `sub`: username
   - `aud`: `microservice-api`
   - `roles`: daftar role, misalnya `ADMIN`
   - expiry
6. Client memakai response `accessToken` sebagai header:

```http
Authorization: Bearer <access-token>
```

7. Gateway dan service tujuan memvalidasi token menggunakan public key dari JWKS Auth Service.
8. Masing-masing backend tetap melakukan authorization. Ini penting: keamanan tidak boleh hanya bergantung pada Gateway.

Catatan local development: RSA key saat ini dibuat ketika `auth-service` startup. Restart container membuat token lama invalid sehingga client perlu login ulang. Ini sengaja dipertahankan sebagai simplifikasi pembelajaran; untuk production, private key harus berasal dari secret mount/KMS/Vault dan mendukung rotasi `kid`.

### 3.3 Request produk/pelanggan

- `GET /api/produk` dan `GET /api/produk/{id}` membaca catalog Produk.
- `POST /api/produk` dan `DELETE /api/produk/{id}` memerlukan role `ADMIN`.
- `GET /api/pelanggan` dan `GET /api/pelanggan/{id}` membaca data Pelanggan.
- `POST /api/pelanggan` dan `DELETE /api/pelanggan/{id}` memerlukan role `ADMIN`.

Database tiap service terpisah. Tidak ada entity JPA lintas-service; komunikasi lintas-service dilakukan melalui HTTP dan discovery.

### 3.4 Order dan integrasi Produk

1. Client mengirim order ke `POST /api/order` melalui Gateway.
2. Order disimpan pada database H2 milik Order.
3. `@PrePersist`/`@PreUpdate` menghitung `total = harga * jumlah`.
4. Order mengirim event teks ke queue `myQueue` RabbitMQ.
5. Untuk `GET /api/order/produk/{id}`, Order:
   - mencari order lokal;
   - mengambil instance `PRODUK` dari Eureka;
   - memanggil `GET /api/produk/{idProduk}` menggunakan `RestTemplate`.
6. Response menggabungkan objek Order lokal dan objek Produk dari service lain.

Perbaikan penting yang dilakukan: konfigurasi Order sekarang membaca `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, dan `RABBITMQ_PASSWORD`. Sebelumnya `spring.rabbitmq.host=localhost` membuat Order mencoba mencari RabbitMQ di container Order sendiri.

### 3.5 RabbitMQ, Produser, dan Konsumer

- `produser` adalah publisher sederhana yang mengirim pesan ke `myQueue`.
- `Order` juga publisher ketika order dibuat/diubah/dihapus.
- `konsumer` mendengarkan `myQueue` dan, untuk pesan `Order created:`, mencoba mengirim email SMTP.
- Konsumer hanya hidup saat profile diaktifkan:

```bash
docker compose --profile notifications up -d
```

Tanpa profile tersebut, RabbitMQ tetap hidup tetapi tidak ada consumer email. Ini bukan error DNS Prometheus; target `konsumer` memang sengaja tidak dimasukkan ke konfigurasi Prometheus default agar Docker DNS tidak mencari container yang tidak dibuat.

---

## 4. Analisis tiap service

## 4.1 `auth-service`

### Fungsi

Auth Service adalah identity/token service. Ia memiliki entity `AppUser`, repository `UserRepository`, proses bootstrap admin, endpoint login, dan endpoint public JWKS.

### File penting

- `auth-service/src/main/resources/application.properties`
- `.../config/SecurityConfig.java`
- `.../token/AuthController.java`
- `.../token/TokenService.java`
- `.../user/BootstrapAdmin.java`
- `.../user/UserRepository.java`

### Hal yang dipelajari

- `PasswordEncoder` menggunakan BCrypt.
- `JwtEncoder` Nimbus menghasilkan token RSA.
- JWKS hanya mengembalikan public key sehingga resource service tidak menerima private key.
- `SessionCreationPolicy.STATELESS` sesuai dengan API token-based.
- Validasi request login menggunakan `@Valid` dan `@NotBlank`.

### Kekurangan dan status

- RSA signing key masih ephemeral. Cocok untuk demo satu container, tetapi token invalid setelah restart dan tidak cocok untuk multi-instance production. **Belum diubah karena scope proyek adalah local learning; sudah diberi komentar batasan.**
- H2 console dan endpoint actuator dibuat mudah diakses untuk development. Jangan expose Auth langsung ke internet.
- Belum ada rate limit/lockout untuk brute-force login. Rate limiting sebaiknya ditambahkan pada Gateway jika materi berikutnya membahas hardening.
- Password bootstrap wajib berasal dari environment, tetapi file `.env` lokal tidak boleh di-commit. `.env` sudah dikeluarkan dari tracking Git dan `.gitignore` diperbarui.

---

## 4.2 `eureka`

### Fungsi

Eureka menyimpan registry instance. Client mendaftarkan nama, host, port, dan statusnya. Gateway dan Order mengambil informasi tersebut untuk menemukan service tanpa hardcode IP container.

### Konfigurasi penting

- Port `8761`.
- `register-with-eureka=false`.
- `fetch-registry=false`.
- Endpoint actuator Prometheus tersedia.

### Kekurangan dan status

- Eureka tidak memiliki authentication layer. Untuk local Docker yang hanya di laptop hal ini masih dapat diterima, tetapi port `8761` sebaiknya tidak dipublish ketika tidak perlu.
- Startup Compose belum sama dengan readiness aplikasi; `service_started` berarti proses sudah dimulai, bukan berarti registry sudah siap menerima semua request. Bila startup race muncul, tunggu beberapa detik atau tambahkan healthcheck HTTP yang sesuai image.
- Tidak memakai IP statis. Ini penting karena IP container dapat berubah setiap recreate.

---

## 4.3 `gateway`

### Fungsi

Gateway adalah entry point dari host pada `http://localhost:9310`. Route yang tersedia:

| Route | Target Eureka |
|---|---|
| `/auth/**` | `AUTH-SERVICE` |
| `/api/produk/**` | `PRODUK` |
| `/api/order/**` | `ORDER` |
| `/api/pelanggan/**` | `PELANGGAN` |
| `/send` | `PRODUSER` |

Gateway memvalidasi JWT resource-server. Endpoint login, health, dan Prometheus dibuat public; route bisnis membutuhkan token.

### Kekurangan dan status

- Gateway dapat hidup sebelum Eureka selesai register sehingga request awal dapat menerima 503/5xx. Tunggu registry siap ketika demo startup.
- Gateway tidak boleh menjadi satu-satunya authorization layer. Backend tetap memvalidasi JWT dan role, dan port backend tidak dipublish ke host.
- Belum ada rate limit, circuit breaker, dan request-size limit. Itu adalah materi hardening lanjutan, bukan syarat agar flow dasar berjalan.

---

## 4.4 `Produk`

### Fungsi

Produk menyimpan catalog `id`, `nama`, `satuan`, dan `harga` pada H2. Endpoint berada di `/api/produk`. Produk mendaftarkan diri ke Eureka sebagai `PRODUK` dan menjadi dependency HTTP bagi Order.

### Kekurangan

- Entity dipakai langsung sebagai request/response, belum memakai DTO.
- Belum ada Bean Validation untuk nama, satuan, dan harga; nilai negatif/kosong dapat masuk.
- Controller memiliki GET, POST, DELETE, tetapi belum memiliki PUT walaupun security mengizinkan PUT.
- H2 console aktif dan `ddl-auto=update` cocok untuk latihan, bukan migrasi production.
- Delete ID yang tidak ada perlu kontrak 404/idempotent yang lebih eksplisit.

### Status runtime

Property discovery memakai placeholder `EUREKA_URL`, dan JWT memakai Auth `9300`. Endpoint metric `/actuator/prometheus` dipantau Prometheus pada target `produk:8081`.

---

## 4.5 `Pelanggan`

### Fungsi

Pelanggan menyimpan data master pelanggan (`id`, `nama`, `alamat`) pada database H2. Service terdaftar di Eureka sebagai `PELANGGAN` dan diakses melalui `/api/pelanggan`.

### Kekurangan

- Belum ada validasi data dan DTO.
- Belum ada endpoint update walaupun aturan security mengizinkan PUT.
- Delete ID yang tidak ada belum dipetakan secara eksplisit.
- H2/password development dan `ddl-auto=update` bukan pilihan production.

### Status runtime

URL Eureka memakai DNS/placeholder, bukan IP bridge Docker statis. JWT resource server menunjuk ke `auth-service:9300`.

---

## 4.6 `Order`

### Fungsi

Order adalah service paling integratif. Ia menyimpan order, menghitung total, menemukan Produk dari Eureka, dan menerbitkan event RabbitMQ.

### Perbaikan yang dilakukan

1. RabbitMQ tidak lagi hardcode ke `localhost`; semua parameter dibaca dari environment.
2. Update menyimpan entity lebih dulu, baru mengirim event `Order updated`.
3. Delete memastikan entity ada, menghapusnya lebih dulu, lalu mengirim event `Order deleted`.
4. Delete terhadap ID yang tidak ada dikembalikan sebagai 404 oleh controller.
5. Import yang tidak digunakan dihapus.

### Kekurangan yang masih terlihat

- Create masih memiliki celah konsistensi: database dapat berhasil disimpan tetapi publish event dapat gagal. Solusi production adalah transactional outbox atau broker transaction, bukan sekadar `try/catch`.
- Queue `myQueue` masih non-durable dan event masih berupa string bebas. Ini sengaja dipertahankan agar materi RabbitMQ dasar mudah dibaca. Pengembangan berikutnya sebaiknya memakai JSON event versioned, durable queue, persistent message, publisher confirm, retry, dan DLQ.
- Request menerima entity JPA langsung; belum ada validasi bahwa `idProduk`, `idPelanggan`, `harga`, dan `jumlah` valid.
- Harga dikirim dari client sehingga secara bisnis dapat dimanipulasi. Sistem nyata seharusnya mengambil harga terpercaya dari Produk dan menerapkan idempotency key.
- `RestTemplate` masih sederhana dan belum dilengkapi timeout/circuit breaker.

---

## 4.7 `produser`

### Fungsi

Produser adalah contoh publisher RabbitMQ minimal. Endpoint `/send` meneruskan message ke queue `myQueue`. Endpoint memerlukan role `ADMIN` pada resource service.

### Kekurangan

- Method yang digunakan adalah GET untuk operasi yang mengubah state. Secara HTTP seharusnya POST.
- Message belum divalidasi ukuran/kosongnya dan masih berupa string bebas.
- Payload dikembalikan dan dicetak ke log; ini tidak baik jika payload mengandung data sensitif.
- Publisher belum menggunakan confirm/retry/DLQ.

### Status runtime

Host RabbitMQ menggunakan DNS `rabbitmq` melalui environment. Service tidak dipublish ke host; akses demo dilakukan melalui Gateway route `/send`.

---

## 4.8 `konsumer`

### Fungsi

Konsumer membaca `myQueue`, memfilter event order yang diawali `Order created:`, melakukan parsing detail order, dan memanggil SMTP menggunakan `JavaMailSender`.

### Kekurangan

- Hanya aktif dengan profile `notifications`.
- Credential SMTP dan alamat tujuan wajib diatur melalui environment; default kosong akan membuat email gagal.
- Parsing berbasis posisi teks rapuh. Event JSON tervalidasi lebih aman.
- Belum ada retry backoff, DLQ, poison-message handling, dan idempotency.
- Payload order dicetak ke stdout.

### Cara mengaktifkan

```bash
MAIL_USERNAME=akun@example.com \
MAIL_PASSWORD=app-password \
MAIL_TO=penerima@example.com \
docker compose --profile notifications up -d --build konsumer
```

Gunakan app password SMTP, bukan password utama akun email.

---

## 4.9 RabbitMQ

RabbitMQ adalah broker asynchronous. Order dan Produser mengirim pesan, Konsumer membaca pesan. Management UI ada pada `http://localhost:15672` dengan credential development default dari image jika tidak diubah.

Plugin `rabbitmq_prometheus` membuka metric internal pada port `15692`; Prometheus men-scrape `rabbitmq:15692`, sehingga tidak perlu publish port tersebut ke host.

Batasan saat ini: queue non-durable, tidak ada DLQ dan publisher confirm. Untuk praktikum dasar hal ini memudahkan, tetapi harus dijelaskan sebagai trade-off saat presentasi.

---

## 4.10 Monitoring metric: Prometheus + Grafana

### Prometheus

Prometheus melakukan pull/scrape setiap 15 detik ke:

- `eureka:8761/actuator/prometheus`
- `auth-service:9300/actuator/prometheus`
- `gateway:9310/actuator/prometheus`
- `produk:8081/actuator/prometheus`
- `order:8082/actuator/prometheus`
- `pelanggan:8083/actuator/prometheus`
- `produser:8080/actuator/prometheus`
- `rabbitmq:15692/metrics`

`konsumer` tidak ada pada target default karena service tersebut tidak dibuat tanpa profile `notifications`. Ini mencegah error DNS Docker saat Prometheus berjalan normal tanpa consumer.

### Grafana

Grafana memakai provisioning datasource Prometheus melalui:

```text
http://prometheus:9090
```

Dashboard `Microservices Overview` menampilkan:

- jumlah service up;
- status RabbitMQ;
- request rate;
- JVM heap usage;
- response 5xx;
- jumlah message RabbitMQ yang ready.

Diagnostic editor seperti `Property datasources is not allowed` adalah false positive schema YAML. `apiVersion` dan `datasources` memang format resmi provisioning Grafana dan tidak boleh dihapus.

### Cara membaca metric

- `up == 1`: target berhasil discrape.
- `up == 0`: Prometheus tidak dapat terhubung atau endpoint gagal.
- `jvm_memory_used_bytes`: penggunaan heap JVM.
- `http_server_requests_seconds_count`: jumlah request yang sudah selesai.
- `rabbitmq_detailed_queue_messages_ready`: pesan menunggu consumer.

---

## 4.11 Monitoring log: ELK

### Alur

1. Setiap service menggunakan `logback-spring.xml`.
2. Logback menulis ke console dan async TCP appender.
3. Appender mengirim event JSON ke `logstash:5000`.
4. Logstash menerima JSON, menambah target index `microservice-YYYY.MM.dd`, lalu mengirim ke Elasticsearch.
5. Kibana membaca index tersebut dari Elasticsearch.

### Cara memakai Kibana

1. Buka `http://localhost:5601`.
2. Buat Data View: `microservice-*`.
3. Pilih time field `@timestamp`.
4. Contoh query:

```text
service : "AUTH-SERVICE"
```

```text
service : "GATEWAY"
```

ELK memonitor **log/event detail**, sedangkan Prometheus memonitor **angka/time-series metric**. Keduanya berbeda fungsi dan saling melengkapi.

### Batasan local development

- Elasticsearch security dimatikan dan berjalan single-node.
- Karena hanya ada satu node, replica shard tidak bisa ditempatkan; status cluster `yellow` dapat normal selama semua primary shard `STARTED`. `red` berbeda: itu berarti primary belum aktif dan harus dianalisis.
- Logstash stdout masih aktif untuk debugging.
- Tidak ada retention/index lifecycle lanjutan selain volume dan konfigurasi dasar.
- Logstash dapat mulai sedikit lebih lambat daripada aplikasi; async appender akan mencoba koneksi kembali.
- Kibana memerlukan waktu initialization cukup lama dan dapat sementara mengembalikan HTTP 503 sebelum migrasi saved objects selesai.

---

## 5. Docker dan resource laptop 8 GB

Root `Dockerfile` adalah multi-stage build:

1. Stage Maven + JDK 17 menyalin source service berdasarkan `SERVICE_DIR`.
2. Maven membuat executable JAR.
3. Stage runtime memakai `eclipse-temurin:17-jre`, sehingga image runtime lebih kecil daripada image build.

Compose menggunakan DNS network internal, bukan IP statis. `.env` hanya untuk local secret dan sudah dikeluarkan dari tracking Git; gunakan `.env.example` sebagai acuan.

Batas memory yang dipasang:

| Service | Limit |
|---|---:|
| RabbitMQ | 160 MB |
| Eureka | 320 MB |
| Auth | 320 MB |
| Produk | 288 MB |
| Order | 352 MB |
| Pelanggan | 288 MB |
| Produser | 288 MB |
| Konsumer | 288 MB |
| Gateway | 352 MB |
| Prometheus | 192 MB |
| Grafana | 192 MB |
| Elasticsearch | 768 MB |
| Logstash | 384 MB |
| Kibana | 640 MB |

- Batas ini mencegah container tumbuh tanpa batas. Namun limit bukan jaminan semua service akan selalu cukup pada beban besar; jika terjadi OOM, lihat `docker inspect` dan naikkan hanya service yang membutuhkan. Kibana terbukti membutuhkan sekitar 384 MB heap saat plugin initialization, sehingga limitnya dibuat 640 MB dengan `NODE_OPTIONS=--max-old-space-size=384`. Pada laptop 8 GB, ELK adalah bagian terberat. Jika hanya ingin belajar JWT/CRUD, jalankan core stack tanpa ELK; jika ingin demo log, jalankan seluruh monitoring.
- Elasticsearch host pada mesin uji memiliki sisa disk sekitar 7,4%, melewati watermark default 90%. Untuk local development Compose menetapkan watermark `low=90%`, `high=95%`, dan `flood_stage=97%`. Ini memberi headroom lokal tanpa mematikan proteksi disk. Jika disk terus menipis, hapus data Docker yang tidak diperlukan; jangan mengandalkan kenaikan watermark sebagai solusi jangka panjang.

---

## 6. Prosedur menjalankan di Linux Mint

### Prasyarat

```bash
docker --version
docker compose version
```

Pastikan Docker daemon aktif dan user memiliki izin menjalankan Docker.

### Konfigurasi local

Buat `.env` dari `.env.example`, lalu isi password development. Jangan commit `.env`.

Minimal Compose membutuhkan:

- `AUTH_BOOTSTRAP_ADMIN_PASSWORD`
- `GRAFANA_ADMIN_PASSWORD`

### Validasi sebelum startup

```bash
docker compose config --quiet
```

Jika secret wajib belum ada, gunakan environment sementara atau isi `.env` lokal.

### Menjalankan core + monitoring

```bash
docker compose up -d --build
```

### Menjalankan consumer email

```bash
docker compose --profile notifications up -d --build
```

### Pemeriksaan status

```bash
docker compose ps
```

### Log service

```bash
docker compose logs --tail=100 auth-service
```

```bash
docker compose logs --tail=100 order
```

```bash
docker compose logs --tail=100 logstash
```

### Pemeriksaan endpoint host

- Eureka: `http://localhost:8761`
- Gateway health: `http://localhost:9310/actuator/health`
- Prometheus: `http://localhost:9090/targets`
- Grafana: `http://localhost:3000`
- Kibana: `http://localhost:5601`
- RabbitMQ: `http://localhost:15672`

Auth login dilakukan melalui Gateway:

```bash
curl -i -X POST http://localhost:9310/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"PASSWORD_DARI_ENV"}'
```

Jangan menggunakan `http://localhost:9000`; port itu bukan bagian dari topology ini dan digunakan Portainer pada laptop pengguna.

### Stop dan reset data local

```bash
docker compose down
```

Untuk menghapus database/metric/log local juga:

```bash
docker compose down -v
```

Perintah kedua menghapus volume Elasticsearch, Grafana, Prometheus, dan data broker yang didefinisikan Compose. Gunakan hanya jika memang ingin reset.

---

## 7. Skenario presentasi kepada dosen

Urutan paling mudah dijelaskan:

1. **Tunjukkan topology Compose**: semua service berada pada satu network, tetapi hanya Gateway dan tools monitoring yang perlu diakses dari host.
2. **Tunjukkan Eureka**: service mendaftarkan diri; Order menemukan Produk melalui nama service, bukan IP.
3. **Login JWT**: minta token dari `/auth/token`, jelaskan `iss`, `sub`, `roles`, dan expiry.
4. **Request tanpa token**: endpoint bisnis mendapat 401.
5. **Request dengan token**: GET berhasil; mutation tanpa role ADMIN mendapat 403; token admin dapat melakukan POST/DELETE.
6. **Tunjukkan integrasi Order-Produk**: `GET /api/order/produk/{id}` memerlukan database Order dan discovery Produk.
7. **Tunjukkan asynchronous flow**: create order menghasilkan pesan RabbitMQ; jika profile notifications aktif, Konsumer mengambilnya.
8. **Tunjukkan Prometheus/Grafana**: bedakan health target, request rate, JVM heap, 5xx, dan queue depth.
9. **Tunjukkan Kibana**: filter log berdasarkan field `service` dan waktu.
10. **Jelaskan batasan**: H2, ephemeral RSA key, queue string/non-durable, serta tidak adanya retry/DLQ adalah keputusan simplifikasi local learning, bukan klaim production-ready.

Kalimat inti yang perlu diingat:

> Eureka menjawab “service ini berada di mana?”, Gateway menjawab “request masuk lewat mana?”, Auth menjawab “siapa pengirimnya?”, backend menjawab “boleh melakukan apa?”, RabbitMQ memisahkan proses asynchronous, Prometheus mengukur angka, dan ELK menelusuri detail log.

---

## 8. Validasi yang telah dijalankan

Validasi final pada repositori:

```text
./auth-service/mvnw -q -f auth-service/pom.xml package -DskipTests
./eureka/mvnw -q -f eureka/pom.xml package -DskipTests
./gateway/mvnw -q -f gateway/pom.xml package -DskipTests
./konsumer/mvnw -q -f konsumer/pom.xml package -DskipTests
./Order/mvnw -q -f Order/pom.xml package -DskipTests
./Pelanggan/mvnw -q -f Pelanggan/pom.xml package -DskipTests
./Produk/mvnw -q -f Produk/pom.xml package -DskipTests
./produser/mvnw -q -f produser/pom.xml package -DskipTests
```

Hasil: semua command berhasil.

Validasi lain:

- `docker compose config --quiet`: berhasil.
- `docker compose build --check`: selesai tanpa warning.
- Build image nyata `docker compose build auth-service eureka gateway konsumer order pelanggan produk produser`: berhasil.
- Smoke test `docker compose up -d --wait`: seluruh container utama berhasil dibuat dan berjalan setelah startup Spring yang lama.
- Setelah startup penuh, Prometheus menunjukkan 8 target Spring/RabbitMQ `up` dan Gateway health mengembalikan HTTP 200 dengan status `UP`.
- Kibana sempat keluar dengan exit 134 karena heap Node 192 MB, lalu 256 MB; setelah dinaikkan menjadi 384 MB Kibana bertahan dan API status mengembalikan HTTP 200.
- Elasticsearch sempat `red` karena disk watermark; setelah watermark local-dev diperbaiki, semua 30 primary shard aktif dan cluster menjadi `yellow` yang wajar untuk single-node.
- Diagnostic editor pada Compose dan file Java/config yang diubah: tidak ada error/warning.
- Pencarian pada source/config (di luar file laporan) untuk referensi lama `auth-service:9000`, `localhost:9000`, dan IP bridge `172.18.0.2`: tidak menemukan hasil.

Status container perlu selalu diverifikasi pada mesin pengguna dengan `docker compose ps`, karena container tidak otomatis dianggap running hanya karena source dan image berhasil dibangun.

---

## 9. Prioritas pengembangan berikutnya

Jika materi dilanjutkan dari local learning menuju production-like design, urutannya:

1. DTO + Bean Validation pada semua boundary HTTP.
2. JSON event versioned, durable queue, persistent message, publisher confirm, retry, dan DLQ.
3. Transactional outbox untuk konsistensi database-event.
4. Persistent RSA key dengan secret management dan key rotation.
5. Database terpisah yang persistent dengan migration tool dan credential secret.
6. Rate limit/brute-force protection di Gateway/Auth.
7. Timeout, circuit breaker, dan observability trace untuk komunikasi Order-Produk.
8. Authentication/authorization Eureka dan pengurangan port host yang dipublish.
9. Test contract/integration menggunakan Testcontainers.
10. Standardisasi versi Spring Boot/Spring Cloud dan pipeline CI.

Prioritas tersebut sengaja tidak semuanya diimplementasikan sekarang agar proyek tetap ringan, mudah dipelajari, dan tidak berubah menjadi boilerplate yang mengaburkan konsep utama mata kuliah.
