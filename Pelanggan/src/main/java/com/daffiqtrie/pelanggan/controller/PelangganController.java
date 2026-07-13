package com.daffiqtrie.pelanggan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daffiqtrie.pelanggan.model.Pelanggan;
import com.daffiqtrie.pelanggan.service.PelangganService;

@RestController
@RequestMapping("/api/pelanggan")
public class PelangganController {
    @Autowired
    private PelangganService pelangganService;

    @GetMapping
    public List<Pelanggan> getAllPelanggan() {
        return pelangganService.getAllPelanggan();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pelanggan> getPelangganById(@PathVariable Integer id) {
        Pelanggan pelanggan = pelangganService.getPelangganById(id);
        return pelanggan != null ? ResponseEntity.ok(pelanggan) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Pelanggan createPelanggan(@RequestBody Pelanggan pelanggan) {
        return pelangganService.createPelanggan(pelanggan);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pelanggan> updatePelanggan(@PathVariable Integer id, @RequestBody Pelanggan pelanggan) {
        Pelanggan updated = pelangganService.updatePelanggan(id, pelanggan);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePelanggan(@PathVariable Integer id) {
        pelangganService.deletePelanggan(id);
        return ResponseEntity.noContent().build();
    }

}
