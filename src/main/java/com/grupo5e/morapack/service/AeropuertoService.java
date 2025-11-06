package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Aeropuerto;

import java.util.List;
import java.util.Optional;

public interface AeropuertoService {
    List<Aeropuerto> listar();
    List<Aeropuerto> listarDisponibles();
    Integer insertar(Aeropuerto aeropuerto);
    Aeropuerto actualizar(Integer id, Aeropuerto aeropuerto);
    Aeropuerto toggleEstado(Integer id);
    Aeropuerto buscarPorId(Integer id);
    Optional<Aeropuerto> buscarPorCodigoIATA(String codigoIATA);
    void eliminar(Integer id);
    boolean existePorId(Integer id);
    List<Aeropuerto> insertarBulk(List<Aeropuerto> aeropuertos);
}
