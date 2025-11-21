package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByPedidoId(Integer pedidoId);
    List<Producto> findByEstado(EstadoProducto estado);
    
    /**
     * Encuentra productos asignados a una instancia de vuelo específica.
     */
    List<Producto> findByInstanciaVueloAsignada(String instanciaVueloAsignada);
    
    /**
     * Encuentra productos cuya instancia de vuelo contiene el texto especificado.
     * Útil para buscar por código de vuelo (e.g., "FL-45").
     */
    List<Producto> findByInstanciaVueloAsignadaContaining(String codigoVuelo);
    
    /**
     * Encuentra productos con instancia de vuelo asignada (no null).
     * Filtra opcionalmente por estado.
     */
    @Query("SELECT p FROM Producto p WHERE p.instanciaVueloAsignada IS NOT NULL")
    List<Producto> findProductosConInstanciaAsignada();
    
    /**
     * Encuentra productos con instancia de vuelo asignada y en estados específicos.
     * Usado para cargar asignaciones existentes en el algoritmo.
     */
    @Query("SELECT p FROM Producto p WHERE p.instanciaVueloAsignada IS NOT NULL " +
           "AND p.estado IN :estados")
    List<Producto> findByInstanciaVueloAsignadaNotNullAndEstadoIn(@Param("estados") List<EstadoProducto> estados);
}
