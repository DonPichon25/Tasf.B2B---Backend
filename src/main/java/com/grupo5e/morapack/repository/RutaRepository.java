package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para Ruta (Route).
 * Actualizado siguiendo patrón Backend con queries avanzadas.
 */
@Repository
public interface RutaRepository extends JpaRepository<Ruta, Integer> {
    
    /**
     * Buscar rutas por aeropuerto origen.
     */
    List<Ruta> findByAeropuertoOrigenId(Integer aeropuertoOrigenId);
    
    /**
     * Buscar rutas por aeropuerto destino.
     */
    List<Ruta> findByAeropuertoDestinoId(Integer aeropuertoDestinoId);
    
    /**
     * Buscar rutas por solución específica.
     */
    List<Ruta> findBySolucionId(Integer solucionId);
    
    /**
     * Buscar rutas que contienen un vuelo específico.
     */
    @Query("SELECT r FROM Ruta r JOIN r.vuelos v WHERE v.id = :vueloId")
    List<Ruta> findByVueloId(@Param("vueloId") Integer vueloId);
    
    /**
     * Buscar rutas que transportan un pedido específico.
     */
    @Query("SELECT r FROM Ruta r JOIN r.pedidos p WHERE p.id = :pedidoId")
    List<Ruta> findByPedidoId(@Param("pedidoId") Integer pedidoId);
    
    /**
     * Buscar rutas en un rango de tiempo.
     */
    @Query("SELECT r FROM Ruta r WHERE r.tiempoTotal BETWEEN :min AND :max ORDER BY r.tiempoTotal ASC")
    List<Ruta> findByTiempoTotalBetween(@Param("min") Double min, @Param("max") Double max);
    
    /**
     * Buscar rutas en un rango de costo.
     */
    @Query("SELECT r FROM Ruta r WHERE r.costoTotal BETWEEN :min AND :max ORDER BY r.costoTotal ASC")
    List<Ruta> findByCostoTotalBetween(@Param("min") Double min, @Param("max") Double max);
    
    /**
     * Buscar rutas entre dos aeropuertos específicos.
     */
    @Query("SELECT r FROM Ruta r WHERE r.aeropuertoOrigen.id = :origenId AND r.aeropuertoDestino.id = :destinoId")
    List<Ruta> findByOrigenAndDestino(@Param("origenId") Integer origenId, @Param("destinoId") Integer destinoId);
    
    /**
     * Obtener rutas ordenadas por costo ascendente.
     */
    List<Ruta> findAllByOrderByCostoTotalAsc();
    
    /**
     * Obtener rutas ordenadas por tiempo ascendente.
     */
    List<Ruta> findAllByOrderByTiempoTotalAsc();
    
    /**
     * Buscar rutas con más de X vuelos.
     */
    @Query("SELECT r FROM Ruta r WHERE r.numeroVuelos >= :minVuelos ORDER BY r.numeroVuelos DESC")
    List<Ruta> findByNumeroVuelosGreaterThanEqual(@Param("minVuelos") Integer minVuelos);
    
    /**
     * Contar rutas por solución.
     */
    @Query("SELECT COUNT(r) FROM Ruta r WHERE r.solucion.id = :solucionId")
    long countBySolucionId(@Param("solucionId") Integer solucionId);
    
    /**
     * Obtener estadísticas de costo promedio por solución.
     */
    @Query("SELECT AVG(r.costoTotal) FROM Ruta r WHERE r.solucion.id = :solucionId")
    Double getAverageCostoBySolucion(@Param("solucionId") Integer solucionId);
}
