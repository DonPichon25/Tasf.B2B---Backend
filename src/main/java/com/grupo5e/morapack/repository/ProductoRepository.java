package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
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
     * Devuelve todos los productos que pertenecen a una lista de pedidos.
     * Nos sirve para obtener los productos afectados por la cancelación
     * a partir de los SegmentoVuelo (que tienen referencia al Pedido).
     */
    List<Producto> findByPedidoIn(Collection<Pedido> pedidos);
    /**
     * Encuentra productos con instancia de vuelo asignada y en estados específicos.
     * Usado para cargar asignaciones existentes en el algoritmo.
     */
    @Query("SELECT p FROM Producto p WHERE p.instanciaVueloAsignada IS NOT NULL " +
           "AND p.estado IN :estados")
    List<Producto> findByInstanciaVueloAsignadaNotNullAndEstadoIn(@Param("estados") List<EstadoProducto> estados);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM productos WHERE tipo_data = 0", nativeQuery = true)
    void eliminarTipoDataCero();

    @Query(value = "select count(id) from productos where tipo_data = 0",nativeQuery = true)
    int contarTipoDataCero();
}
