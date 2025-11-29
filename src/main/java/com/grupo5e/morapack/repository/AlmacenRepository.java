package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Almacen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AlmacenRepository extends JpaRepository<Almacen, Integer> {
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM almacenes WHERE tipo_data = 0", nativeQuery = true)
    void eliminarTipoDataCero();
}

