package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.PlanViaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para PlanViaje
 */
@Repository
public interface PlanViajeRepository extends JpaRepository<PlanViaje, Integer> {
    
    /**
     * Buscar todos los planes de un pedido específico
     */
    List<PlanViaje> findByPedidoId(Integer pedidoId);
    
    /**
     * Buscar planes por estado
     */
    List<PlanViaje> findByEstado(String estado);
    
    /**
     * Buscar planes por algoritmo usado
     */
    List<PlanViaje> findByAlgoritmoUsado(String algoritmoUsado);
    
    /**
     * Buscar planes en un rango de fechas
     */
    @Query("SELECT p FROM PlanViaje p WHERE p.fechaPlanificacion BETWEEN :inicio AND :fin")
    List<PlanViaje> findByFechaPlanificacionBetween(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin
    );
    
    /**
     * Buscar el plan más reciente de un pedido
     */
    @Query("SELECT p FROM PlanViaje p WHERE p.pedido.id = :pedidoId ORDER BY p.createdAt DESC")
    List<PlanViaje> findMostRecentByPedidoId(@Param("pedidoId") Integer pedidoId);
    
    /**
     * Contar planes por estado
     */
    long countByEstado(String estado);
    
    /**
     * Buscar planes con más de X segmentos
     */
    @Query("SELECT p FROM PlanViaje p WHERE SIZE(p.segmentosVuelo) >= :minSegmentos")
    List<PlanViaje> findPlanesConMinSegmentos(@Param("minSegmentos") int minSegmentos);
}

