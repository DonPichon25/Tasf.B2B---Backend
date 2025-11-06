package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.SegmentoVuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Buscar segmentos de un pedido
     */
    List<SegmentoVuelo> findByPedidoId(Integer pedidoId);
    
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

