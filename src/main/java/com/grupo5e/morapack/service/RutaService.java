package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Ruta;

import java.util.List;

public interface RutaService {
    List<Ruta> listar();
    Integer insertar(Ruta ruta);
    Ruta actualizar(Integer id, Ruta ruta);
    Ruta buscarPorId(Integer id);
    List<Ruta> buscarPorAeropuertoOrigen(Integer aeropuertoId);
    List<Ruta> buscarPorAeropuertoDestino(Integer aeropuertoId);
    void eliminar(Integer id);
    boolean existePorId(Integer id);
    List<Ruta> insertarBulk(List<Ruta> rutas);
    
    // Nuevos métodos siguiendo patrón Backend
    List<Ruta> buscarPorSolucionId(Integer solucionId);
    List<Ruta> buscarPorVueloId(Integer vueloId);
    List<Ruta> buscarPorPedidoId(Integer pedidoId);
    List<Ruta> buscarPorRangoTiempo(Double min, Double max);
    List<Ruta> buscarPorRangoCosto(Double min, Double max);
    List<Ruta> buscarPorOrigenYDestino(Integer origenId, Integer destinoId);
}
