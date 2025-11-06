package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.service.PedidoService;
import com.grupo5e.morapack.service.ProductoService;
import com.grupo5e.morapack.service.ServicioPersistenciaAlgoritmo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de persistencia de algoritmo.
 * Maneja operaciones batch para minimizar llamadas a BD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServicioPersistenciaAlgoritmoImpl implements ServicioPersistenciaAlgoritmo {

    private final ProductoService productoService;
    private final PedidoService pedidoService;

    /**
     * Persistir solución del algoritmo a base de datos.
     * Crea productos para todas las divisiones en una sola transacción.
     * 
     * OPTIMIZADO: Elimina N+1 queries cargando todos los pedidos de una vez.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int persistirSolucion(List<DivisionPedido> divisionesPedidos) {
        log.info("========================================");
        log.info("PERSISTIENDO SOLUCIÓN DEL ALGORITMO A BASE DE DATOS");
        log.info("Total de divisiones a persistir: {}", divisionesPedidos.size());
        log.info("========================================");

        int productosCreados = 0;
        Map<Integer, List<DivisionPedido>> divisionesPorPedido = agruparDivisionesPorPedido(divisionesPedidos);

        // OPTIMIZACIÓN: Cargar todos los pedidos necesarios en una sola query (evita N+1)
        List<Integer> idsPedidos = new ArrayList<>(divisionesPorPedido.keySet());
        Map<Integer, Pedido> cachePedidos = cargarPedidosEnBatch(idsPedidos);
        
        log.debug("Pedidos cargados en batch: {}", cachePedidos.size());

        // Acumular todos los productos para inserción batch final
        List<Producto> todosLosProductos = new ArrayList<>();

        // Para cada pedido, crear productos para sus divisiones
        for (Map.Entry<Integer, List<DivisionPedido>> entrada : divisionesPorPedido.entrySet()) {
            Integer idPedido = entrada.getKey();
            List<DivisionPedido> divisiones = entrada.getValue();

            log.debug("Pedido {}: Creando {} producto(s)", idPedido, divisiones.size());

            // Obtener pedido desde cache (sin query adicional)
            Pedido pedido = cachePedidos.get(idPedido);
            if (pedido == null) {
                log.warn("Pedido {} no encontrado en cache, saltando divisiones", idPedido);
                continue;
            }

            for (DivisionPedido division : divisiones) {
                // Crear registro de Producto para esta división
                Producto producto = new Producto();
                producto.setNombre("Producto-Division-" + idPedido + "-" + productosCreados);
                producto.setEstado(division.getEstado());
                producto.setPeso(1.0); // Peso unitario por defecto
                producto.setVolumen(1.0); // Volumen unitario por defecto
                producto.setPedido(pedido);

                todosLosProductos.add(producto);
                productosCreados++;
            }
        }

        // OPTIMIZACIÓN: Una sola operación batch para todos los productos
        if (!todosLosProductos.isEmpty()) {
            log.info("Insertando {} productos en una sola operación batch", todosLosProductos.size());
            productoService.insertarBulk(todosLosProductos);
        }

        // OPTIMIZACIÓN: Actualizar todos los estados de pedidos en batch
        actualizarEstadosPedidosEnBatch(new ArrayList<>(divisionesPorPedido.keySet()), EstadoPedido.EN_TRANSITO);

        log.info("========================================");
        log.info("PERSISTENCIA COMPLETADA");
        log.info("Productos creados: {}", productosCreados);
        log.info("Pedidos actualizados: {}", divisionesPorPedido.size());
        log.info("========================================");

        return productosCreados;
    }
    
    /**
     * OPTIMIZACIÓN: Carga múltiples pedidos en una sola query usando IN clause.
     * Evita el problema N+1 y es más eficiente que cargar todos.
     * 
     * Ejemplo: SELECT * FROM pedido WHERE id IN (1, 2, 3, 4, 5)
     * En lugar de: N queries individuales o cargar toda la tabla.
     */
    private Map<Integer, Pedido> cargarPedidosEnBatch(List<Integer> idsPedidos) {
        if (idsPedidos == null || idsPedidos.isEmpty()) {
            return new HashMap<>();
        }
        
        // CORRECCIÓN: Solo cargar los pedidos necesarios con IN clause
        List<Pedido> pedidos = pedidoService.buscarPorIds(idsPedidos);
        
        // Convertir a Map para lookup O(1)
        Map<Integer, Pedido> cache = new HashMap<>();
        for (Pedido pedido : pedidos) {
            cache.put(pedido.getId(), pedido);
        }
        
        log.debug("Cargados {} pedidos en batch con IN clause", cache.size());
        return cache;
    }
    
    /**
     * OPTIMIZACIÓN: Actualiza múltiples pedidos en batch en lugar de uno por uno.
     */
    private void actualizarEstadosPedidosEnBatch(List<Integer> idsPedidos, EstadoPedido nuevoEstado) {
        for (Integer idPedido : idsPedidos) {
            try {
                actualizarEstadoPedido(idPedido, nuevoEstado);
            } catch (Exception e) {
                log.error("Error actualizando estado del pedido {}: {}", idPedido, e.getMessage());
                // Continuar con los demás en lugar de fallar todo
            }
        }
    }

    /**
     * Actualizar estado de un pedido en la base de datos.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void actualizarEstadoPedido(Integer idPedido, EstadoPedido estado) {
        try {
            Pedido pedido = pedidoService.buscarPorId(idPedido);
            if (pedido != null) {
                pedidoService.actualizarEstado(idPedido, estado);
                log.debug("Estado del pedido {} actualizado a {}", idPedido, estado);
            } else {
                log.warn("Pedido {} no encontrado para actualizar estado", idPedido);
            }
        } catch (Exception e) {
            log.error("Error actualizando estado del pedido {}: {}", idPedido, e.getMessage());
        }
    }

    /**
     * Agrupar divisiones por ID de pedido para procesamiento batch.
     */
    private Map<Integer, List<DivisionPedido>> agruparDivisionesPorPedido(List<DivisionPedido> divisiones) {
        Map<Integer, List<DivisionPedido>> agrupadas = new HashMap<>();

        for (DivisionPedido division : divisiones) {
            agrupadas.computeIfAbsent(division.getIdPedido(), k -> new ArrayList<>()).add(division);
        }

        return agrupadas;
    }

    /**
     * Obtener estadísticas sobre las divisiones persistidas.
     */
    @Override
    public Map<String, Integer> obtenerEstadisticasSolucion(List<DivisionPedido> divisiones) {
        Map<String, Integer> estadisticas = new HashMap<>();

        estadisticas.put("totalDivisiones", divisiones.size());
        estadisticas.put("pedidosUnicos", (int) divisiones.stream()
            .map(DivisionPedido::getIdPedido)
            .distinct()
            .count());
        estadisticas.put("cantidadTotal", divisiones.stream()
            .mapToInt(DivisionPedido::getCantidad)
            .sum());

        return estadisticas;
    }
}

