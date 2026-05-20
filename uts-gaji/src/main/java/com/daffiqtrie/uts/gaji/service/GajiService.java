package com.daffiqtrie.uts.gaji.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daffiqtrie.uts.gaji.repository.GajiRepository;
import com.daffiqtrie.uts.gaji.model.Gaji;

@Service
public class GajiService {
    @Autowired
    private GajiRepository gajiRepository;

    public List<Gaji> getAllGaji() {
        return gajiRepository.findAll();
    }

    public Gaji getGajiById(Long id) {
        return gajiRepository.findById(id).orElse(null);
    }

    public Gaji createGaji(Gaji gaji) {
        return gajiRepository.save(gaji);
    }

    public void deleteGaji(Long id) {
        gajiRepository.deleteById(id);
    }

    public Gaji updateGaji(Long id, Gaji gaji) {
        Gaji existingGaji = getGajiById(id);
        existingGaji.setNama(gaji.getNama());
        existingGaji.setJabatan(gaji.getJabatan());
        existingGaji.setGaji(gaji.getGaji());
        return gajiRepository.save(existingGaji);
    }

}
