package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    
    // ===================================================================
    // QUERIES BÁSICAS (sin productos - RÁPIDAS)
    // ===================================================================
    
    List<Pedido> findByClienteId(Long clienteId);
    List<Pedido> findByEstado(EstadoPedido estado);
    Optional<Pedido> findByExternalId(String externalId);

    /**
     * Busca pedidos dentro de una ventana de tiempo (para escenarios diarios/semanales)
     * SIN cargar productos (LAZY) - OPTIMIZADO
     * 
     * @param fechaInicio Fecha de inicio de la ventana (inclusive)
     * @param fechaFin Fecha de fin de la ventana (inclusive)
     * @return Lista de pedidos cuya fechaPedido está dentro del rango
     */
    List<Pedido> findByFechaPedidoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
    
    // ===================================================================
    // QUERIES CON EntityGraph (cargar productos SOLO cuando se necesiten)
    // ===================================================================
    
    /**
     * OPTIMIZACIÓN: Carga pedidos CON productos en UNA SOLA query (evita N+1)
     * Usa esto cuando NECESITES los productos
     */
    @EntityGraph(attributePaths = {"productos", "cliente"})
    @Query("SELECT p FROM Pedido p WHERE p.fechaPedido BETWEEN :fechaInicio AND :fechaFin")
    List<Pedido> findByFechaPedidoBetweenConProductos(
        @Param("fechaInicio") LocalDateTime fechaInicio, 
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    /**
     * OPTIMIZACIÓN: Carga UN pedido con productos (evita N+1)
     */
    @EntityGraph(attributePaths = {"productos", "cliente"})
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Pedido findByIdConProductos(@Param("id") Integer id);
    
    /**
     * OPTIMIZACIÓN: Carga pedidos por estado CON productos
     */
    @EntityGraph(attributePaths = {"productos"})
    @Query("SELECT p FROM Pedido p WHERE p.estado = :estado")
    List<Pedido> findByEstadoConProductos(@Param("estado") EstadoPedido estado);
    
    // ===================================================================
    // QUERIES DE CONTEO (ultra-rápidas, sin JOINs)
    // ===================================================================
    
    /**
     * Cuenta pedidos sin cargar datos - ULTRA RÁPIDO
     */
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.fechaPedido BETWEEN :fechaInicio AND :fechaFin")
    long contarPedidosEnRango(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    /**
     * Suma total de productos SIN cargar la lista - ULTRA RÁPIDO
     * Usa el campo cantidadProductos directo
     */
    @Query("SELECT SUM(p.cantidadProductos) FROM Pedido p WHERE p.fechaPedido BETWEEN :fechaInicio AND :fechaFin")
    Long sumarCantidadProductosEnRango(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );
    
    // ===================================================================
    // PAGINACIÓN (para cargas grandes)
    // ===================================================================
    
    /**
     * OPTIMIZACIÓN: Carga pedidos paginados (para datasets grandes)
     * SIN productos (LAZY)
     */
    Page<Pedido> findByFechaPedidoBetween(
        LocalDateTime fechaInicio, 
        LocalDateTime fechaFin,
        Pageable pageable
    );
    
    /**
     * OPTIMIZACIÓN: Carga pedidos paginados CON productos (si es necesario)
     */
    @EntityGraph(attributePaths = {"productos"})
    @Query("SELECT p FROM Pedido p WHERE p.fechaPedido BETWEEN :fechaInicio AND :fechaFin")
    Page<Pedido> findByFechaPedidoBetweenConProductos(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin,
        Pageable pageable
    );

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM pedidos WHERE tipo_data = 0", nativeQuery = true)
    void eliminarTipoDataCero();

    @Query(value = "select count(id) from pedidos where tipo_data = 0",nativeQuery = true)
    int contarTipoDataCero();
    @Query("SELECT COALESCE(MAX(p.id), 0) FROM Pedido p WHERE p.tipoData = :tipoData")
    Integer findMaxIdByTipoData(@Param("tipoData") int tipoData);
}
