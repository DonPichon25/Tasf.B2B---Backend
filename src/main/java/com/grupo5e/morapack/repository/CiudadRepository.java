package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Ciudad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface CiudadRepository extends JpaRepository<Ciudad, Integer> {
    Optional<Ciudad> findByCodigo(String codigo);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ciudades WHERE tipo_data = 0", nativeQuery = true)
    void eliminarTipoDataCero();

    @Query(value = "select count(id) from ciudades where tipo_data = 0",nativeQuery = true)
    int contarTipoDataCero();
}
