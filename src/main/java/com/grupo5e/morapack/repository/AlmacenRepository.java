package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlmacenRepository extends JpaRepository<Almacen, Integer> {
}

