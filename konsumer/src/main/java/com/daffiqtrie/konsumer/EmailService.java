package com.daffiqtrie.konsumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service yang bertanggung jawab untuk mengirim email notifikasi.
 * Menggunakan JavaMailSender dari Spring Boot Starter Mail
 * untuk mengirim email melalui SMTP server yang dikonfigurasi
 * di application.properties.
 */
@Service
public class EmailService {

    // JavaMailSender otomatis dikonfigurasi oleh Spring Boot
    // berdasarkan properti spring.mail.* di application.properties
    @Autowired
    private JavaMailSender mailSender;

    /**
     * Mengirim email notifikasi order ke alamat email yang dituju.
     *
     * @param to      alamat email tujuan (email pemesan)
     * @param subject judul email yang akan dikirim
     * @param body    isi email yang berisi detail order
     */
    public void sendOrderNotification(String to, String subject, String body) {
        // Membuat objek SimpleMailMessage untuk email teks biasa
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        // Mengirim email melalui SMTP server
        mailSender.send(message);
        System.out.println("Email notifikasi berhasil dikirim ke: " + to);
    }
}
