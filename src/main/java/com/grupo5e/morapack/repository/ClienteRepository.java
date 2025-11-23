package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Cliente;
import com.grupo5e.morapack.core.model.UsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, UsuarioId> {
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    Optional<Cliente> findByCorreo(String correo);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM clientes WHERE tipo_data = 0", nativeQuery = true)
    void eliminarTipoDataCero();

    @Query(value = "select count(id) from clientes where tipo_data = 0",nativeQuery = true)
    int contarTipoDataCero();
}
