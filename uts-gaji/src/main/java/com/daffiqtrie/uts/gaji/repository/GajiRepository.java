package com.daffiqtrie.uts.gaji.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.daffiqtrie.uts.gaji.model.Gaji;

@Repository

public interface GajiRepository extends JpaRepository<Gaji, Long> {

}
