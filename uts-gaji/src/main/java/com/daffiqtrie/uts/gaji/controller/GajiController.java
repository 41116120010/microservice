package com.daffiqtrie.uts.gaji.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daffiqtrie.uts.gaji.model.Gaji;
import com.daffiqtrie.uts.gaji.service.GajiService;

@RestController
@RequestMapping("/api/gaji")
public class GajiController {

    @Autowired
    private GajiService gajiService;

    @GetMapping
    public List<Gaji> getAllGaji() {
        return gajiService.getAllGaji();
    }

    @GetMapping("/{id}")
    public Gaji getGajiById(@PathVariable Long id) {
        return gajiService.getGajiById(id);
    }

    @PostMapping
    public Gaji createGaji(@RequestBody Gaji gaji) {
        return gajiService.createGaji(gaji);
    }

    @DeleteMapping("/{id}")
    public void deleteGaji(@PathVariable Long id) {
        gajiService.deleteGaji(id);
    }

    @PutMapping("/{id}")
    public Gaji updateGaji(@PathVariable Long id, @RequestBody Gaji gaji) {
        return gajiService.updateGaji(id, gaji);
    }

}
