# Microservice Learning Project

Repositori ini berisi tugas mata kuliah Pemrograman Microservice.

## Dokumentasi utama

- [Laporan analisis, flow, temuan, perbaikan, dan panduan demo](LAPORAN_ANALISIS.md)

Stack utama menggunakan Eureka, Gateway, JWT, RabbitMQ, Prometheus, Grafana, Elasticsearch, Logstash, dan Kibana. Seluruh service ditujukan untuk local development melalui Docker Compose; Auth Service berjalan internal pada port `9300`, sedangkan entry point host adalah Gateway pada port `9310`.
