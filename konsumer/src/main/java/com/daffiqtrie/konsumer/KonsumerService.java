package com.daffiqtrie.konsumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KonsumerService {

    // Inject EmailService untuk mengirim email notifikasi
    @Autowired
    private EmailService emailService;

    // Mengambil alamat email tujuan dari application.properties
    @Value("${app.mail.to}")
    private String emailTujuan;

    /**
     * Method yang mendengarkan pesan dari RabbitMQ pada queue "myQueue".
     * Ketika ada pesan masuk (misalnya dari Order Service saat order baru dibuat),
     * method ini akan memproses pesan tersebut dan mengirim email notifikasi
     * ke alamat email yang sudah dikonfigurasi di application.properties.
     *
     * @param message pesan string yang diterima dari RabbitMQ
     */
    @RabbitListener(queues = "myQueue")
    public void receiveMessage(String message) {
        System.out.println("Received message: " + message);

        // Hanya proses pesan yang berisi informasi order baru
        if (message.startsWith("Order created:")) {
            // Menyusun subject email notifikasi
            String subject = "Notifikasi Order Baru";

            // Menyusun isi email dengan detail order dari pesan RabbitMQ
            String body = buildEmailBody(message);

            // Mengirim email notifikasi ke alamat email yang sudah ditentukan
            emailService.sendOrderNotification(emailTujuan, subject, body);
        }
    }

    /**
     * Mengekstrak nilai dari pesan berdasarkan key yang diberikan.
     * Contoh: dari pesan "ID: 1 ID Produk: 2",
     * extractValue(message, "ID:") akan mengembalikan "1"
     *
     * @param message pesan lengkap dari RabbitMQ
     * @param key     key yang ingin diekstrak nilainya
     * @return nilai yang diekstrak, atau null jika key tidak ditemukan
     */
    private String extractValue(String message, String key) {
        // Mencari posisi key dalam pesan
        int startIndex = message.indexOf(key);
        if (startIndex == -1) {
            return null;
        }

        // Mengambil substring setelah key
        startIndex += key.length();
        String remaining = message.substring(startIndex).trim();

        // Mengambil nilai sampai spasi berikutnya atau akhir string
        int endIndex = remaining.indexOf(" ");
        if (endIndex == -1) {
            return remaining;
        }
        return remaining.substring(0, endIndex);
    }

    /**
     * Menyusun isi email berdasarkan data yang diekstrak dari pesan RabbitMQ.
     * Format email dibuat agar mudah dibaca oleh penerima.
     *
     * @param message pesan lengkap dari RabbitMQ yang berisi detail order
     * @return string isi email yang sudah diformat
     */
    private String buildEmailBody(String message) {
        // Mengekstrak setiap detail order dari pesan
        String orderId = extractValue(message, "ID:");
        String idProduk = extractValue(message, "ID Produk:");
        String jumlah = extractValue(message, "Jumlah:");
        String hargaSatuan = extractValue(message, "Harga Satuan:");
        String totalHarga = extractValue(message, "Total Harga:");
        String idPelanggan = extractValue(message, "ID Pelanggan:");

        // Menyusun isi email dalam format yang rapi
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
}
