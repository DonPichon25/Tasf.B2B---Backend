package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Ciudad;

import java.util.List;
import java.util.Optional;

public interface CiudadService {
    List<Ciudad> listar();
    Integer insertar(Ciudad ciudad);
    Ciudad actualizar(Integer id, Ciudad ciudad);
    Ciudad buscarPorId(Integer id);
    Optional<Ciudad> buscarPorCodigo(String codigo);
    void eliminar(Integer id);
    boolean existePorId(Integer id);
    List<Ciudad> insertarBulk(List<Ciudad> ciudades);
    void limpiarBD();

}
