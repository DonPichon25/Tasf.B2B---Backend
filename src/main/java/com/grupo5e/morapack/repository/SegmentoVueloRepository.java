package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.SegmentoVuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para SegmentoVuelo
 */
@Repository
public interface SegmentoVueloRepository extends JpaRepository<SegmentoVuelo, Integer> {
    
    /**
     * Buscar segmentos de un plan específico
     */
    List<SegmentoVuelo> findByPlanViajeIdOrderByOrdenSegmentoAsc(Integer planViajeId);
    /**
     * Devuelve los segmentos que corresponden a:
     *  - un vuelo base específico (vuelo_id)
     *  - con una hora de salida estimada exacta (la misma que la instancia)
     *
     * Esta será la forma de saber qué pedido(s) y, por ende,
     * qué producto(s) iban en UNA instancia de vuelo concreta.
     */
    @Query("""
           SELECT s
           FROM SegmentoVuelo s
           WHERE s.vuelo.id = :vueloId
             AND s.horaSalidaEstimada = :horaSalida
           """)
    List<SegmentoVuelo> findByVueloAndHoraSalidaExacta(
            @Param("vueloId") Integer vueloId,
            @Param("horaSalida") LocalDateTime horaSalidaEstimada
    );
    /**
     * Buscar segmentos de un pedido
     */
    List<SegmentoVuelo> findByPedidoId(Integer pedidoId);
    @Modifying
    @Transactional
    @Query("DELETE FROM SegmentoVuelo s WHERE s.planViaje.fechaPlanificacion >= :desde")
    int deleteByPlanFechaFrom(@Param("desde") LocalDateTime desde);
    /**
     * Buscar segmentos que salen en un rango de tiempo
     */
    @Query("SELECT s FROM SegmentoVuelo s WHERE s.horaSalidaEstimada BETWEEN :inicio AND :fin")
    List<SegmentoVuelo> findBySalidaBetween(
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );
    
    /**
     * Buscar segmentos por vuelo específico
     */
    List<SegmentoVuelo> findByVueloId(Integer vueloId);
    
    /**
     * Buscar segmentos por ruta (origen-destino)
     */
    @Query("SELECT s FROM SegmentoVuelo s WHERE s.codigoOrigen = :origen AND s.codigoDestino = :destino")
    List<SegmentoVuelo> findByRuta(
        @Param("origen") String codigoOrigen,
        @Param("destino") String codigoDestino
    );
}

