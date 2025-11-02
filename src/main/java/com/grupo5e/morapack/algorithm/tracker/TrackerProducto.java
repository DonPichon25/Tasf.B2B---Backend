package com.grupo5e.morapack.algorithm.tracker;

import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.constants.Constantes;

import java.util.*;

/**
 * Tracker de productos para seguimiento granular a nivel de producto individual.
 * 
 * Permite rastrear cada producto individualmente, no solo a nivel de pedido,
 * facilitando reportes detallados y APIs que requieren información a nivel producto.
 * 
 * Características:
 * - Registra todos los productos de todos los pedidos al inicio
 * - Asigna rutas de vuelos a productos cuando se asigna un pedido
 * - Mantiene estado de cada producto (EN_ALMACEN, EN_VUELO, ENTREGADO)
 * - Provee solución a nivel producto: Map<Producto, ArrayList<Vuelo>>
 * - Genera resumen de tracking con estadísticas
 */
public class TrackerProducto {

    // Mapa principal: Producto -> Ruta asignada (lista de vuelos)
    private Map<Producto, ArrayList<Vuelo>> productoRutaMap;

    // Mapa de estado: Producto -> Estado actual
    private Map<Producto, EstadoProducto> productoEstadoMap;

    // Set de productos registrados para validación
    private Set<Producto> productosRegistrados;

    // Contador de productos por estado
    private Map<EstadoProducto, Integer> contadorEstados;

    public TrackerProducto() {
        this.productoRutaMap = new HashMap<>();
        this.productoEstadoMap = new HashMap<>();
        this.productosRegistrados = new HashSet<>();
        this.contadorEstados = new HashMap<>();
        
        // Inicializar contadores
        for (EstadoProducto estado : EstadoProducto.values()) {
            contadorEstados.put(estado, 0);
        }
    }

    /**
     * Inicializa el tracker registrando todos los productos de todos los pedidos.
     * 
     * @param pedidos Lista de pedidos de los cuales extraer productos
     */
    public void inicializarDesdePedidos(List<Pedido> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            System.out.println("⚠️ TrackerProducto: No hay pedidos para inicializar");
            return;
        }

        int totalProductos = 0;

        for (Pedido pedido : pedidos) {
            if (pedido == null) {
                continue;
            }

            List<Producto> productos = pedido.getProductos();
            
            // Si el pedido no tiene productos explícitos, crear uno virtual
            if (productos == null || productos.isEmpty()) {
                // Para pedidos sin productos, asumir 1 producto virtual
                // Esto mantiene compatibilidad con el sistema
                continue;
            }

            for (Producto producto : productos) {
                if (producto != null) {
                    productosRegistrados.add(producto);
                    productoEstadoMap.put(producto, EstadoProducto.EN_ALMACEN);
                    productoRutaMap.put(producto, new ArrayList<>()); // Ruta vacía inicialmente
                    contadorEstados.put(EstadoProducto.EN_ALMACEN, 
                                      contadorEstados.get(EstadoProducto.EN_ALMACEN) + 1);
                    totalProductos++;
                }
            }
        }

        System.out.println("✅ TrackerProducto inicializado: " + totalProductos + " productos registrados");
    }

    /**
     * Asigna una ruta a un pedido y actualiza el tracking de todos sus productos.
     * 
     * Si la ruta está vacía, significa que el pedido ya está en su destino.
     * 
     * @param pedido Pedido al cual asignar la ruta
     * @param ruta Lista de vuelos que conforman la ruta asignada
     */
    public void asignarPedidoARuta(Pedido pedido, ArrayList<Vuelo> ruta) {
        if (pedido == null) {
            return;
        }

        List<Producto> productos = pedido.getProductos();
        
        // Si el pedido no tiene productos explícitos, no hay nada que rastrear
        if (productos == null || productos.isEmpty()) {
            // Para pedidos sin productos explícitos, no se puede hacer tracking a nivel producto
            // El tracking se mantiene a nivel pedido solamente
            return;
        }

        // Actualizar ruta y estado para cada producto del pedido
        for (Producto producto : productos) {
            if (producto == null || !productosRegistrados.contains(producto)) {
                continue;
            }

            // Actualizar ruta asignada
            if (ruta != null) {
                // Copiar la ruta (no compartir referencia)
                productoRutaMap.put(producto, new ArrayList<>(ruta));
            } else {
                // Ruta nula significa sin asignar
                productoRutaMap.put(producto, new ArrayList<>());
            }

            // Actualizar estado según la ruta
            actualizarEstadoProducto(producto, ruta);
        }

        if (Constantes.LOGGING_VERBOSO) {
            System.out.println("  Productos del pedido " + pedido.getId() + 
                             " asignados a ruta con " + (ruta != null ? ruta.size() : 0) + " vuelos");
        }
    }

    /**
     * Actualiza el estado de un producto basado en su ruta asignada.
     * 
     * @param producto Producto a actualizar
     * @param ruta Ruta asignada al producto
     */
    private void actualizarEstadoProducto(Producto producto, ArrayList<Vuelo> ruta) {
        if (producto == null) {
            return;
        }

        EstadoProducto estadoAnterior = productoEstadoMap.getOrDefault(producto, EstadoProducto.EN_ALMACEN);
        
        // Actualizar contador: decrementar estado anterior
        contadorEstados.put(estadoAnterior, 
                          Math.max(0, contadorEstados.get(estadoAnterior) - 1));

        EstadoProducto nuevoEstado;

        if (ruta == null || ruta.isEmpty()) {
            // Ruta vacía: producto ya en destino o sin asignar
            Pedido pedido = producto.getPedido();
            if (pedido != null && pedido.getAeropuertoOrigenCodigo() != null &&
                pedido.getAeropuertoDestinoCodigo() != null &&
                pedido.getAeropuertoOrigenCodigo().equals(pedido.getAeropuertoDestinoCodigo())) {
                // Mismo origen y destino = ya entregado
                nuevoEstado = EstadoProducto.ENTREGADO;
            } else {
                // Sin ruta asignada = aún en almacén
                nuevoEstado = EstadoProducto.EN_ALMACEN;
            }
        } else {
            // Con ruta asignada: el producto estará en vuelo durante el transporte
            // Por ahora, marcar como EN_VUELO si tiene ruta
            // En tiempo de ejecución real, el estado se calcularía dinámicamente
            nuevoEstado = EstadoProducto.EN_VUELO;
        }

        // Actualizar estado y contador
        productoEstadoMap.put(producto, nuevoEstado);
        contadorEstados.put(nuevoEstado, contadorEstados.get(nuevoEstado) + 1);
    }

    /**
     * Obtiene la ruta asignada a un producto específico.
     * 
     * @param producto Producto del cual obtener la ruta
     * @return Lista de vuelos asignados, o lista vacía si no tiene ruta
     */
    public ArrayList<Vuelo> obtenerRutaProducto(Producto producto) {
        if (producto == null) {
            return new ArrayList<>();
        }
        return productoRutaMap.getOrDefault(producto, new ArrayList<>());
    }

    /**
     * Obtiene el estado actual de un producto.
     * 
     * @param producto Producto del cual obtener el estado
     * @return Estado actual del producto, o EN_ALMACEN por defecto
     */
    public EstadoProducto obtenerEstadoProducto(Producto producto) {
        if (producto == null) {
            return EstadoProducto.EN_ALMACEN;
        }
        return productoEstadoMap.getOrDefault(producto, EstadoProducto.EN_ALMACEN);
    }

    /**
     * Obtiene la solución completa a nivel producto.
     * 
     * Retorna un mapa donde cada producto tiene asignada su ruta de vuelos.
     * Útil para APIs y reportes que requieren información a nivel producto.
     * 
     * @return Map<Producto, ArrayList<Vuelo>> solución a nivel producto
     */
    public Map<Producto, ArrayList<Vuelo>> obtenerSolucionNivelProducto() {
        // Retornar copia defensiva para evitar modificaciones externas
        Map<Producto, ArrayList<Vuelo>> solucion = new HashMap<>();
        
        for (Map.Entry<Producto, ArrayList<Vuelo>> entry : productoRutaMap.entrySet()) {
            // Solo incluir productos con rutas asignadas (no vacías)
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                solucion.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        
        return solucion;
    }

    /**
     * Obtiene el número total de productos registrados.
     * 
     * @return Cantidad de productos registrados
     */
    public int getTotalProductos() {
        return productosRegistrados.size();
    }

    /**
     * Obtiene el número de productos con ruta asignada.
     * 
     * @return Cantidad de productos con ruta asignada
     */
    public int getProductosAsignados() {
        int count = 0;
        for (ArrayList<Vuelo> ruta : productoRutaMap.values()) {
            if (ruta != null && !ruta.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Imprime un resumen detallado del tracking de productos.
     * 
     * Muestra estadísticas por estado, productos asignados, y distribución de rutas.
     */
    public void imprimirResumenTracking() {
        System.out.println("\n========== RESUMEN DE TRACKING DE PRODUCTOS ==========");
        System.out.println("Total de productos registrados: " + productosRegistrados.size());
        System.out.println("Productos con ruta asignada: " + getProductosAsignados());
        System.out.println("Productos sin asignar: " + (productosRegistrados.size() - getProductosAsignados()));

        System.out.println("\n----- Distribución por Estado -----");
        for (EstadoProducto estado : EstadoProducto.values()) {
            int cantidad = contadorEstados.getOrDefault(estado, 0);
            double porcentaje = productosRegistrados.size() > 0 
                ? (cantidad * 100.0) / productosRegistrados.size() 
                : 0.0;
            System.out.println("  " + estado + ": " + cantidad + " (" + 
                             String.format("%.1f", porcentaje) + "%)");
        }

        // Estadísticas de rutas
        Map<Integer, Integer> distribucionRutas = new HashMap<>();
        int rutasVacias = 0;

        for (ArrayList<Vuelo> ruta : productoRutaMap.values()) {
            if (ruta == null || ruta.isEmpty()) {
                rutasVacias++;
            } else {
                int numVuelos = ruta.size();
                distribucionRutas.put(numVuelos, 
                                    distribucionRutas.getOrDefault(numVuelos, 0) + 1);
            }
        }

        System.out.println("\n----- Distribución de Rutas -----");
        System.out.println("  Sin ruta asignada: " + rutasVacias);
        
        List<Integer> tamanosOrdenados = new ArrayList<>(distribucionRutas.keySet());
        Collections.sort(tamanosOrdenados);
        
        for (Integer tamano : tamanosOrdenados) {
            int cantidad = distribucionRutas.get(tamano);
            String tipoRuta = tamano == 1 ? "directa" : 
                            tamano == 2 ? "1 escala" : 
                            tamano == 3 ? "2 escalas" : 
                            tamano + " vuelos";
            System.out.println("  Ruta " + tipoRuta + ": " + cantidad + " productos");
        }

        // Calcular productos únicos en vuelos
        Set<Vuelo> vuelosUtilizados = new HashSet<>();
        for (ArrayList<Vuelo> ruta : productoRutaMap.values()) {
            if (ruta != null) {
                vuelosUtilizados.addAll(ruta);
            }
        }
        System.out.println("\nVuelos únicos utilizados: " + vuelosUtilizados.size());

        System.out.println("===================================================\n");
    }

    /**
     * Limpia todo el tracking, útil para resetear el estado.
     * 
     * Reinicia todos los mapas y contadores a estado inicial.
     */
    public void limpiar() {
        productoRutaMap.clear();
        productoEstadoMap.clear();
        productosRegistrados.clear();
        
        for (EstadoProducto estado : EstadoProducto.values()) {
            contadorEstados.put(estado, 0);
        }
        
        if (Constantes.LOGGING_VERBOSO) {
            System.out.println("TrackerProducto limpiado");
        }
    }

    /**
     * Verifica si un producto está registrado en el tracker.
     * 
     * @param producto Producto a verificar
     * @return true si está registrado, false en caso contrario
     */
    public boolean estaRegistrado(Producto producto) {
        return producto != null && productosRegistrados.contains(producto);
    }

    /**
     * Obtiene todos los productos registrados.
     * 
     * @return Set con todos los productos registrados
     */
    public Set<Producto> getProductosRegistrados() {
        return new HashSet<>(productosRegistrados); // Copia defensiva
    }
}
