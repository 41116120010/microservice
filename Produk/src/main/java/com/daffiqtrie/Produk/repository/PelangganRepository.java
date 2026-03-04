package com.daffiqtrie.Produk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.daffiqtrie.Produk.model.Pelanggan;

@Repository
public interface PelangganRepository extends JpaRepository<Pelanggan, Integer> {

}
