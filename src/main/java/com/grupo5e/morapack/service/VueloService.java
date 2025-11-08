package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.enums.EstadoVuelo;

import java.util.List;
import java.util.Optional;

public interface VueloService {
    List<Vuelo> listar();
    Integer insertar(Vuelo vuelo);
    Vuelo actualizar(Integer id, Vuelo vuelo);
    Vuelo buscarPorId(Integer id);
    List<Vuelo> buscarPorRuta(Integer origenId, Integer destinoId);
    List<Vuelo> buscarPorEstado(EstadoVuelo estado);
    List<Vuelo> buscarDisponibles(Integer capacidadMinima);
    Optional<Vuelo> buscarPorIdentificador(String identificador);
    void eliminar(Integer id);
    boolean existePorId(Integer id);
    List<Vuelo> insertarBulk(List<Vuelo> vuelos);
}
