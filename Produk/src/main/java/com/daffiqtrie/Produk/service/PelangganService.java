package com.daffiqtrie.Produk.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daffiqtrie.Produk.model.Pelanggan;
import com.daffiqtrie.Produk.repository.PelangganRepository;

@Service
public class PelangganService {

    @Autowired
    private PelangganRepository pelangganRepository;

    public List<Pelanggan> getAllPelanggan() {
        return pelangganRepository.findAll();
    }

    public Pelanggan getPelangganById(Integer id) {
        return pelangganRepository.findById(id).orElse(null);
    }

    public Pelanggan createPelanggan(Pelanggan pelanggan) {
        return pelangganRepository.save(pelanggan);
    }

    public void deletePelanggan(Integer id) {
        pelangganRepository.deleteById(id);
    }
}
