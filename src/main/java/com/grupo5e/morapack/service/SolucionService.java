package com.grupo5e.morapack.service;

import com.grupo5e.morapack.api.dto.EstadisticasSolucionesDTO;
import com.grupo5e.morapack.api.dto.SolucionDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar soluciones del algoritmo ALNS.
 * Siguiendo patrón Backend para Solution
 */
public interface SolucionService {
    
    /**
     * Crear una nueva solución
     */
    Integer crearSolucion(SolucionDTO solucionDTO);
    
    /**
     * Obtener solución por ID
     */
    SolucionDTO obtenerPorId(Integer id);
    
    /**
     * Obtener todas las soluciones
     */
    List<SolucionDTO> obtenerTodas();
    
    /**
     * Obtener soluciones por algoritmo usado
     */
    List<SolucionDTO> obtenerPorAlgoritmo(String algoritmo);
    
    /**
     * Obtener soluciones en un rango de costo
     */
    List<SolucionDTO> obtenerPorRangoCosto(Double min, Double max);
    
    /**
     * Obtener soluciones en un rango de tiempo
     */
    List<SolucionDTO> obtenerPorRangoTiempo(Double min, Double max);
    
    /**
     * Obtener soluciones en un rango de fitness
     */
    List<SolucionDTO> obtenerPorRangoFitness(Double min, Double max);
    
    /**
     * Obtener soluciones con máximo de paquetes no entregados
     */
    List<SolucionDTO> obtenerPorMaxNoEntregados(Integer max);
    
    /**
     * Obtener la mejor solución (mayor fitness)
     */
    SolucionDTO obtenerMejorSolucion();
    
    /**
     * Obtener las N mejores soluciones
     */
    List<SolucionDTO> obtenerTopMejores(int limite);
    
    /**
     * Obtener soluciones creadas en un rango de fechas
     */
    List<SolucionDTO> obtenerPorRangoFechas(LocalDateTime inicio, LocalDateTime fin);
    
    /**
     * Obtener soluciones perfectas (0 no entregados)
     */
    List<SolucionDTO> obtenerSolucionesPerfectas();
    
    /**
     * Obtener las últimas N soluciones
     */
    List<SolucionDTO> obtenerUltimasSoluciones(int limite);
    
    /**
     * Actualizar una solución
     */
    void actualizarSolucion(Integer id, SolucionDTO solucionDTO);
    
    /**
     * Eliminar una solución
     */
    void eliminarSolucion(Integer id);
    
    /**
     * Obtener estadísticas agregadas de todas las soluciones
     */
    EstadisticasSolucionesDTO obtenerEstadisticas();
    
    /**
     * Obtener estadísticas por algoritmo
     */
    EstadisticasSolucionesDTO obtenerEstadisticasPorAlgoritmo(String algoritmo);
}

