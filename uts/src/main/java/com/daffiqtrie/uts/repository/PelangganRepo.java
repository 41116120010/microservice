package com.daffiqtrie.uts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.daffiqtrie.uts.models.PelangganModels;

@Repository
public interface PelangganRepo extends JpaRepository<PelangganModels, Integer> {

}
