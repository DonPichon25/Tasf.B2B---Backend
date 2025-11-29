package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.enums.EstadoVuelo;

import java.time.LocalDateTime;
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
    void limpiarBD();
    int contarTipoDataCero();
    
    /**
     * Valida si un vuelo puede ser cancelado (no ha despegado aún).
     * 
     * Un vuelo solo puede cancelarse si:
     * - No tiene productos en estado IN_TRANSIT (ya despegó)
     * - El tiempo actual de simulación es anterior a la hora de salida
     * 
     * @param vueloId ID del vuelo
     * @param tiempoSimulacionActual Tiempo actual de la simulación
     * @return true si el vuelo puede cancelarse, false si ya despegó
     * @throws IllegalStateException si el vuelo no existe
     */
    boolean puedeSerCancelado(Integer vueloId, LocalDateTime tiempoSimulacionActual);
}
