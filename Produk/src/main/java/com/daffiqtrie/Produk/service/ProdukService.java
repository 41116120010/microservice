package com.daffiqtrie.Produk.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daffiqtrie.Produk.repository.ProdukRepository;
import com.daffiqtrie.Produk.model.Produk;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProdukService {
    @Autowired
    private ProdukRepository produkRepository;

    public List<Produk> getAllProduk() {
        log.info("Fetching all products");
        return produkRepository.findAll();
    }

    public Produk getProdukById(Long id) {
        log.info("Fetching product with id: {}", id);
        return produkRepository.findById(id).orElse(null);
    }

    public Produk createProduk(Produk produk) {
        log.info("Creating product: {}", produk);
        return produkRepository.save(produk);
    }

    public Produk updateProduk(Long id, Produk produk) {
        log.info("Updating product with id: {}", id);
        return produkRepository.findById(id)
                .map(existing -> {
                    existing.setNama(produk.getNama());
                    existing.setSatuan(produk.getSatuan());
                    existing.setHarga(produk.getHarga());
                    return produkRepository.save(existing);
                })
                .orElse(null);
    }

    public void deleteProduk(Long id) {
        log.info("Deleting product with id: {}", id);
        produkRepository.deleteById(id);
    }

}
