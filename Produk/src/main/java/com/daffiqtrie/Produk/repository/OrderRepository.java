package com.daffiqtrie.Produk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.daffiqtrie.Produk.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    void deleteByIdPelanggan(Integer idPelanggan);

    List<Order> findByIdPelanggan(Integer idPelanggan);
}
