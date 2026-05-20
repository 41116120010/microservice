package com.daffiqtrie.uts.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daffiqtrie.uts.models.PelangganModels;
import com.daffiqtrie.uts.services.PelangganServices;

@RestController
@RequestMapping("/uts/pelanggan")
public class PelangganControllers {
    @Autowired
    private PelangganServices pelangganServices;

    @GetMapping
    public List<PelangganModels> getAllPelanggan() {
        return pelangganServices.getAllPelanggan();
    }

    @GetMapping("/{id}")
    public PelangganModels getPelangganById(@PathVariable Integer id) {
        return pelangganServices.getPelangganById(id);
    }

    @PostMapping
    public PelangganModels createPelanggan(@RequestBody PelangganModels pelanggan) {
        return pelangganServices.createPelanggan(pelanggan);
    }

    @DeleteMapping("/{id}")
    public void deletePelanggan(@PathVariable Integer id) {
        pelangganServices.deletePelanggan(id);
    }

}
