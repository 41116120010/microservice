package com.daffiqtrie.order.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer idProduk;
    private Integer idPelanggan;
    private Long harga;
    private Long jumlah;
    private Long total;

    @PrePersist
    @PreUpdate
    public void calculateTotal() {
        if (this.harga != null && this.jumlah != null) {
            this.total = this.harga * this.jumlah;
        }
    }

}
