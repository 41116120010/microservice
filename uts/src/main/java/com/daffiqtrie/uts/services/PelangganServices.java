package com.daffiqtrie.uts.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daffiqtrie.uts.models.PelangganModels;
import com.daffiqtrie.uts.repository.PelangganRepo;

@Service
public class PelangganServices {

    @Autowired
    private PelangganRepo pelangganRepo;

    public List<PelangganModels> getAllPelanggan() {
        return pelangganRepo.findAll();
    }

    public PelangganModels getPelangganById(Integer id) {
        return pelangganRepo.findById(id).orElse(null);
    }

    public PelangganModels createPelanggan(PelangganModels pelanggan) {
        return pelangganRepo.save(pelanggan);
    }

    public void deletePelanggan(Integer id) {
        pelangganRepo.deleteById(id);
    }

}
