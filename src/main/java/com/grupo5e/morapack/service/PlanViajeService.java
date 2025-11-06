package com.grupo5e.morapack.service;

import com.grupo5e.morapack.api.dto.EstadisticasPlanesDTO;
import com.grupo5e.morapack.api.dto.PlanViajeDTO;
import com.grupo5e.morapack.api.dto.SegmentoVueloDTO;

import java.util.List;

/**
 * Servicio para gestionar planes de viaje.
 * Siguiendo patrón Backend para TravelPlan
 */
public interface PlanViajeService {
    
    /**
     * Crear un nuevo plan de viaje
     */
    Integer crearPlanViaje(PlanViajeDTO planViajeDTO);
    
    /**
     * Obtener plan de viaje por ID
     */
    PlanViajeDTO obtenerPorId(Integer id);
    
    /**
     * Obtener todos los planes de viaje
     */
    List<PlanViajeDTO> obtenerTodos();
    
    /**
     * Obtener planes de un pedido específico
     */
    List<PlanViajeDTO> obtenerPorPedidoId(Integer pedidoId);
    
    /**
     * Obtener planes por estado
     */
    List<PlanViajeDTO> obtenerPorEstado(String estado);
    
    /**
     * Obtener el plan más reciente de un pedido
     */
    PlanViajeDTO obtenerPlanMasRecientePorPedido(Integer pedidoId);
    
    /**
     * Actualizar estado de un plan
     */
    void actualizarEstado(Integer planId, String nuevoEstado);
    
    /**
     * Eliminar plan de viaje
     */
    void eliminar(Integer id);
    
    /**
     * Obtener segmentos de un plan
     */
    List<SegmentoVueloDTO> obtenerSegmentosPorPlan(Integer planId);
    
    /**
     * Obtener estadísticas de planes
     */
    EstadisticasPlanesDTO obtenerEstadisticas();
}

