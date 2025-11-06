package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.model.Vuelo;

import java.util.List;
import java.util.Map;

/**
 * Servicio para persistir resultados del algoritmo a la base de datos.
 * 
 * Este servicio maneja:
 * - Creación de registros de Producto desde divisiones de pedidos
 * - Asignación de vuelos a pedidos/productos
 * - Actualización de estados de pedidos y productos
 * - Operaciones batch para minimizar llamadas a BD
 */
public interface ServicioPersistenciaAlgoritmo {

    /**
     * Representa una porción dividida de un pedido.
     * Cuando un pedido es muy grande para un solo vuelo, se divide en múltiples partes.
     * Cada división se convierte en un registro de Producto en la base de datos.
     */
    class DivisionPedido {
        private Integer idPedido;
        private Integer cantidad;  // Número de ítems en esta división
        private List<Vuelo> vuelosAsignados;
        private EstadoProducto estado;

        public DivisionPedido(Integer idPedido, Integer cantidad, List<Vuelo> vuelos) {
            this.idPedido = idPedido;
            this.cantidad = cantidad;
            this.vuelosAsignados = vuelos;
            this.estado = EstadoProducto.EN_VUELO;
        }

        public Integer getIdPedido() { return idPedido; }
        public Integer getCantidad() { return cantidad; }
        public List<Vuelo> getVuelosAsignados() { return vuelosAsignados; }
        public EstadoProducto getEstado() { return estado; }

        public void setIdPedido(Integer idPedido) { this.idPedido = idPedido; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
        public void setVuelosAsignados(List<Vuelo> vuelosAsignados) { this.vuelosAsignados = vuelosAsignados; }
        public void setEstado(EstadoProducto estado) { this.estado = estado; }
    }

    /**
     * Persistir solución del algoritmo a la base de datos.
     * Crea registros de Producto para todas las divisiones de pedidos.
     *
     * @param divisionesPedidos Lista de divisiones de pedidos creadas durante la ejecución del algoritmo
     * @return Número de registros de productos creados
     */
    int persistirSolucion(List<DivisionPedido> divisionesPedidos);

    /**
     * Actualizar estado de un pedido en la base de datos.
     *
     * @param idPedido ID del pedido a actualizar
     * @param estado Nuevo estado del pedido
     */
    void actualizarEstadoPedido(Integer idPedido, EstadoPedido estado);

    /**
     * Obtener estadísticas sobre las divisiones de pedidos.
     *
     * @param divisiones Lista de divisiones
     * @return Mapa con estadísticas (totalDivisiones, pedidosUnicos, cantidadTotal)
     */
    Map<String, Integer> obtenerEstadisticasSolucion(List<DivisionPedido> divisiones);
}

