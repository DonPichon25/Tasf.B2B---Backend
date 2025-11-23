package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.EstadoAeropuerto;
import com.grupo5e.morapack.core.model.Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AeropuertoRepository extends JpaRepository<Aeropuerto, Integer> {
    Optional<Aeropuerto> findByCodigoIATA(String codigoIATA);
    List<Aeropuerto> findByEstado(EstadoAeropuerto estado);
    @Query(
            value = "SELECT * FROM aeropuertos WHERE tipo_data = :tipoData",
            nativeQuery = true
    )
    List<Aeropuerto> listarPorTipoData(@Param("tipoData") int tipoData);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM aeropuertos WHERE tipo_data = 0", nativeQuery = true)
    void eliminarTipoDataCero();

    @Query(value = "select count(id) from aeropuertos where tipo_data = 0",nativeQuery = true)
    int contarTipoDataCero();
}
