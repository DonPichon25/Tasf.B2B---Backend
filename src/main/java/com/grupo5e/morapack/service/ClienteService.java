package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteService {
    List<Cliente> listar();
    Long insertar(Cliente cliente);
    Cliente actualizar(Long id, int tipoData,Cliente cliente);
    Cliente buscarPorId(Long idCliente,int tipoData);
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    Optional<Cliente> findByCorreo(String correo);
    void eliminar(Long id,int tipoData);
    boolean existePorId(Long id,int tipoData);
    List<Cliente> insertarBulk(List<Cliente> clientes);
}
