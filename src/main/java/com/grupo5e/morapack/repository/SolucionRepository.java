package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.Solucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para Solucion (Solution).
 * Siguiendo patrón Backend para queries de soluciones del algoritmo.
 */
@Repository
public interface SolucionRepository extends JpaRepository<Solucion, Integer> {
    
    /**
     * Buscar soluciones por algoritmo usado.
     */
    List<Solucion> findByAlgoritmoUsado(String algoritmoUsado);
    
    /**
     * Buscar soluciones en un rango de costo.
     */
    @Query("SELECT s FROM Solucion s WHERE s.costoTotal BETWEEN :min AND :max ORDER BY s.costoTotal ASC")
    List<Solucion> findByCostoTotalBetween(@Param("min") Double min, @Param("max") Double max);
    
    /**
     * Buscar soluciones en un rango de tiempo.
     */
    @Query("SELECT s FROM Solucion s WHERE s.tiempoTotal BETWEEN :min AND :max ORDER BY s.tiempoTotal ASC")
    List<Solucion> findByTiempoTotalBetween(@Param("min") Double min, @Param("max") Double max);
    
    /**
     * Buscar soluciones en un rango de fitness.
     */
    @Query("SELECT s FROM Solucion s WHERE s.fitness BETWEEN :min AND :max ORDER BY s.fitness DESC")
    List<Solucion> findByFitnessBetween(@Param("min") Double min, @Param("max") Double max);
    
    /**
     * Buscar soluciones con un máximo de paquetes no entregados.
     */
    @Query("SELECT s FROM Solucion s WHERE s.paquetesNoEntregados <= :max ORDER BY s.paquetesNoEntregados ASC")
    List<Solucion> findByPaquetesNoEntregadosLessThanEqual(@Param("max") Integer max);
    
    /**
     * Obtener todas las soluciones ordenadas por fitness descendente (mejor primero).
     */
    @Query("SELECT s FROM Solucion s ORDER BY s.fitness DESC")
    List<Solucion> findAllOrderByFitnessDesc();
    
    /**
     * Obtener la mejor solución (mayor fitness).
     */
    @Query("SELECT s FROM Solucion s ORDER BY s.fitness DESC")
    Optional<Solucion> findTopByOrderByFitnessDesc();
    
    /**
     * Obtener las N mejores soluciones.
     */
    @Query("SELECT s FROM Solucion s ORDER BY s.fitness DESC")
    List<Solucion> findTopNByOrderByFitnessDesc(@Param("limit") int limit);
    
    /**
     * Buscar soluciones creadas en un rango de fechas.
     */
    @Query("SELECT s FROM Solucion s WHERE s.createdAt BETWEEN :inicio AND :fin ORDER BY s.createdAt DESC")
    List<Solucion> findByCreatedAtBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    
    /**
     * Obtener soluciones con 0 paquetes no entregados (soluciones perfectas).
     */
    List<Solucion> findByPaquetesNoEntregados(Integer paquetesNoEntregados);
    
    /**
     * Contar soluciones por algoritmo.
     */
    long countByAlgoritmoUsado(String algoritmoUsado);
    
    /**
     * Obtener estadísticas agregadas de fitness.
     */
    @Query("SELECT AVG(s.fitness) FROM Solucion s WHERE s.algoritmoUsado = :algoritmo")
    Double getAverageFitnessByAlgoritmo(@Param("algoritmo") String algoritmo);
    
    /**
     * Obtener estadísticas agregadas de costo.
     */
    @Query("SELECT AVG(s.costoTotal) FROM Solucion s WHERE s.algoritmoUsado = :algoritmo")
    Double getAverageCostoByAlgoritmo(@Param("algoritmo") String algoritmo);
    
    /**
     * Obtener estadísticas agregadas de tiempo.
     */
    @Query("SELECT AVG(s.tiempoTotal) FROM Solucion s WHERE s.algoritmoUsado = :algoritmo")
    Double getAverageTiempoByAlgoritmo(@Param("algoritmo") String algoritmo);
    
    /**
     * Buscar soluciones con más de X rutas generadas.
     */
    @Query("SELECT s FROM Solucion s WHERE s.totalRutas >= :minRutas ORDER BY s.totalRutas DESC")
    List<Solucion> findByTotalRutasGreaterThanEqual(@Param("minRutas") Integer minRutas);
    
    /**
     * Obtener las últimas N soluciones creadas.
     */
    @Query("SELECT s FROM Solucion s ORDER BY s.createdAt DESC")
    List<Solucion> findLatestSolutions(@Param("limit") int limit);
}

