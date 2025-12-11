package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.service.PedidoService;
import com.grupo5e.morapack.service.ProductoService;
import com.grupo5e.morapack.service.VueloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * API REST para consultar resultados del algoritmo.
 * El frontend usa estos endpoints para obtener el estado actual desde la base de datos
 * en lugar de recibir productRoutes en la respuesta del algoritmo.
 * 
 * Equivalente a OrderQueryAPI.java del Backend.
 */
@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
@Tag(name = "Consultas de Pedidos", description = "Endpoints para consultar estado de pedidos, productos y vuelos después de ejecutar el algoritmo")
@CrossOrigin(origins = "*")
public class ConsultasPedidosController {

    private final PedidoService pedidoService;
    private final ProductoService productoService;
    private final VueloService vueloService;

    /**
     * Obtener pedidos dentro de una ventana de tiempo de simulación.
     *
     * @param horaInicio Inicio de ventana de tiempo (formato ISO: 2025-01-02T00:00:00)
     * @param horaFin Fin de ventana de tiempo (formato ISO: 2025-01-02T01:00:00)
     * @return Lista de pedidos creados dentro de la ventana de tiempo
     *
     * Ejemplo: GET /api/consultas/pedidos?horaInicio=2025-01-02T00:00:00&horaFin=2025-01-02T01:00:00
     */
    @GetMapping("/pedidos")
    @Operation(
        summary = "Obtener pedidos por ventana de tiempo",
        description = "Retorna pedidos creados dentro de una ventana de tiempo específica. " +
                      "Útil para consultar resultados del escenario diario o semanal."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pedidos obtenidos exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error al obtener pedidos")
    })
    public ResponseEntity<Map<String, Object>> obtenerPedidosPorVentanaTiempo(
            @Parameter(description = "Hora de inicio (ISO 8601)", example = "2025-01-02T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime horaInicio,
            @Parameter(description = "Hora de fin (ISO 8601)", example = "2025-01-02T01:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime horaFin) {

        try {
            List<Pedido> todosPedidos = pedidoService.listar();

            // Filtrar por ventana de tiempo si se proporciona
            List<Pedido> pedidosFiltrados = todosPedidos;
            if (horaInicio != null || horaFin != null) {
                pedidosFiltrados = todosPedidos.stream()
                    .filter(pedido -> {
                        LocalDateTime fechaPedido = pedido.getFechaPedido();
                        if (fechaPedido == null) return false;

                        boolean despuesInicio = horaInicio == null || !fechaPedido.isBefore(horaInicio);
                        boolean antesFin = horaFin == null || !fechaPedido.isAfter(horaFin);

                        return despuesInicio && antesFin;
                    })
                    .collect(Collectors.toList());
            }

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("exito", true);
            respuesta.put("totalPedidos", pedidosFiltrados.size());
            respuesta.put("pedidos", pedidosFiltrados);
            respuesta.put("ventanaTiempo", Map.of(
                "horaInicio", horaInicio != null ? horaInicio.toString() : "no especificada",
                "horaFin", horaFin != null ? horaFin.toString() : "no especificada"
            ));

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, Object> respuestaError = new HashMap<>();
            respuestaError.put("exito", false);
            respuestaError.put("mensaje", "Error al obtener pedidos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(respuestaError);
        }
    }

    /**
     * Obtener divisiones de producto para un pedido específico.
     *
     * @param idPedido ID del pedido
     * @return Lista de productos (divisiones) para el pedido
     *
     * Ejemplo: GET /api/consultas/productos/12345
     */
    @GetMapping("/productos/{idPedido}")
    @Operation(
        summary = "Obtener productos de un pedido",
        description = "Retorna las divisiones de producto para un pedido específico. " +
                      "Cuando un pedido se divide, cada división se convierte en un producto."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Productos obtenidos exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error al obtener productos")
    })
    public ResponseEntity<Map<String, Object>> obtenerProductosPorPedido(
            @Parameter(description = "ID del pedido", example = "12345")
            @PathVariable Integer idPedido) {

        try {
            List<Producto> productosDelPedido = productoService.buscarPorPedido(idPedido);

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("exito", true);
            respuesta.put("idPedido", idPedido);
            respuesta.put("cantidadProductos", productosDelPedido.size());
            respuesta.put("productos", productosDelPedido);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, Object> respuestaError = new HashMap<>();
            respuestaError.put("exito", false);
            respuestaError.put("mensaje", "Error al obtener productos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(respuestaError);
        }
    }

    /**
     * Obtener todos los productos con sus vuelos asignados.
     *
     * @return Lista de todos los productos con estado y asignaciones de vuelo
     *
     * Ejemplo: GET /api/consultas/productos
     */
    @GetMapping("/productos")
    @Operation(
        summary = "Obtener todos los productos",
        description = "Retorna todos los productos con estadísticas por estado"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Productos obtenidos exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error al obtener productos")
    })
    public ResponseEntity<Map<String, Object>> obtenerTodosProductos() {

        try {
            List<Producto> todosProductos = productoService.listar();

            // Agrupar por estado
            Map<String, Long> conteoPorEstado = todosProductos.stream()
                .collect(Collectors.groupingBy(
                    producto -> producto.getEstado() != null ? producto.getEstado().toString() : "DESCONOCIDO",
                    Collectors.counting()
                ));

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("exito", true);
            respuesta.put("totalProductos", todosProductos.size());
            respuesta.put("productos", todosProductos);
            respuesta.put("desglosePorEstado", conteoPorEstado);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, Object> respuestaError = new HashMap<>();
            respuestaError.put("exito", false);
            respuestaError.put("mensaje", "Error al obtener productos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(respuestaError);
        }
    }

    /**
     * Obtener estado de asignación de vuelos.
     * Retorna estadísticas sobre asignaciones de productos a vuelos.
     *
     * @return Resumen de asignaciones de vuelo
     *
     * Ejemplo: GET /api/consultas/vuelos/estado
     */
    @GetMapping("/vuelos/estado")
    @Operation(
        summary = "Obtener estado de asignaciones de vuelo",
        description = "Retorna estadísticas sobre productos asignados a vuelos"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado obtenido exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error al obtener estado")
    })
    public ResponseEntity<Map<String, Object>> obtenerEstadoVuelos() {

        try {
            List<Producto> todosProductos = productoService.listar();

            // Contar productos por estado de asignación
            long productosAsignados = todosProductos.stream()
                .filter(producto -> producto.getEstado() != null &&
                                  (producto.getEstado().name().equals("EN_VUELO") ||
                                   producto.getEstado().name().equals("ENTREGADO")))
                .count();

            long productosNoAsignados = todosProductos.size() - productosAsignados;

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("exito", true);
            respuesta.put("totalProductos", todosProductos.size());
            respuesta.put("productosAsignados", productosAsignados);
            respuesta.put("productosNoAsignados", productosNoAsignados);
            respuesta.put("tasaAsignacion", todosProductos.size() > 0 ?
                (double) productosAsignados / todosProductos.size() * 100 : 0.0);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, Object> respuestaError = new HashMap<>();
            respuestaError.put("exito", false);
            respuestaError.put("mensaje", "Error al obtener estado de vuelos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(respuestaError);
        }
    }

    /**
     * Obtener estado de un pedido por ID.
     *
     * @param idPedido ID del pedido
     * @return Detalles del pedido con productos
     *
     * Ejemplo: GET /api/consultas/pedidos/12345
     */
    @GetMapping("/pedidos/{idPedido}")
    @Operation(
        summary = "Obtener detalles de un pedido",
        description = "Retorna detalles de un pedido específico con sus productos"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pedido obtenido exitosamente"),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error al obtener pedido")
    })
    public ResponseEntity<Map<String, Object>> obtenerEstadoPedido(
            @Parameter(description = "ID del pedido", example = "12345")
            @PathVariable Integer idPedido) {

        try {
            // Obtener pedido
            Pedido pedido = pedidoService.buscarPorId(idPedido);

            if (pedido == null) {
                Map<String, Object> respuestaError = new HashMap<>();
                respuestaError.put("exito", false);
                respuestaError.put("mensaje", "Pedido no encontrado: " + idPedido);
                return ResponseEntity.notFound().build();
            }

            // Obtener productos del pedido
            List<Producto> productosDelPedido = productoService.buscarPorPedido(idPedido);

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("exito", true);
            respuesta.put("pedido", pedido);
            respuesta.put("cantidadProductos", productosDelPedido.size());
            respuesta.put("productos", productosDelPedido);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, Object> respuestaError = new HashMap<>();
            respuestaError.put("exito", false);
            respuestaError.put("mensaje", "Error al obtener pedido: " + e.getMessage());
            return ResponseEntity.internalServerError().body(respuestaError);
        }
    }

    /**
     * Obtener todos los vuelos.
     *
     * @return Lista de todos los vuelos en el sistema
     *
     * Ejemplo: GET /api/consultas/vuelos
     */
    @GetMapping("/vuelos")
    @Operation(
        summary = "Obtener todos los vuelos",
        description = "Retorna todos los vuelos con estadísticas por estado"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vuelos obtenidos exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error al obtener vuelos")
    })
    public ResponseEntity<Map<String, Object>> obtenerTodosVuelos() {

        try {
            List<Vuelo> todosVuelos = vueloService.listar();

            // Agrupar por estado
            Map<String, Long> conteoPorEstado = todosVuelos.stream()
                .collect(Collectors.groupingBy(
                    vuelo -> vuelo.getEstado() != null ? vuelo.getEstado().toString() : "DESCONOCIDO",
                    Collectors.counting()
                ));

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("exito", true);
            respuesta.put("totalVuelos", todosVuelos.size());
            respuesta.put("vuelos", todosVuelos);
            respuesta.put("desglosePorEstado", conteoPorEstado);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, Object> respuestaError = new HashMap<>();
            respuestaError.put("exito", false);
            respuestaError.put("mensaje", "Error al obtener vuelos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(respuestaError);
        }
    }

    /**
     * Obtener tracking detallado de un pedido.
     * Retorna información completa del pedido incluyendo:
     * - Estado actual del pedido
     * - Si está en vuelo, información del vuelo actual
     * - Lista de productos y su ubicación
     * - Timeline de eventos asociados
     *
     * @param idPedido ID del pedido
     * @return Información completa de tracking
     *
     * Ejemplo: GET /api/consultas/pedidos/12345/tracking
     */
    @GetMapping("/pedidos/{idPedido}/tracking")
    @Operation(
        summary = "Obtener tracking detallado de un pedido",
        description = "Retorna información completa del pedido para tracking, incluyendo estado actual, " +
                      "vuelo actual si está en tránsito, y ubicación de productos."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tracking obtenido exitosamente"),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error al obtener tracking")
    })
    public ResponseEntity<Map<String, Object>> obtenerTrackingDetallado(
            @Parameter(description = "ID del pedido", example = "12345")
            @PathVariable Integer idPedido) {

        try {
            // Obtener pedido
            Pedido pedido = pedidoService.buscarPorId(idPedido);

            if (pedido == null) {
                Map<String, Object> respuestaError = new HashMap<>();
                respuestaError.put("exito", false);
                respuestaError.put("mensaje", "Pedido no encontrado: " + idPedido);
                respuestaError.put("orderId", idPedido);
                return ResponseEntity.status(404).body(respuestaError);
            }

            // Obtener productos del pedido
            List<Producto> productosDelPedido = productoService.buscarPorPedido(idPedido);

            // Determinar el estado general del pedido basado en los productos
            String estadoGeneral = determinarEstadoGeneral(productosDelPedido);
            boolean isInFlight = estadoGeneral.equals("EN_VUELO");

            // Si hay productos en vuelo, buscar información del vuelo actual
            Map<String, Object> currentFlightInfo = null;
            if (isInFlight) {
                currentFlightInfo = obtenerInfoVueloActual(productosDelPedido);
            }

            // Construir lista de productos con ubicación
            List<Map<String, Object>> productosConUbicacion = productosDelPedido.stream()
                .map(p -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", p.getId());
                    info.put("nombre", p.getNombre());
                    info.put("estado", p.getEstado() != null ? p.getEstado().toString() : "DESCONOCIDO");
                    info.put("instanciaVuelo", p.getInstanciaVueloAsignada());
                    info.put("fechaLlegada", p.getFechaHoraLlegada());
                    return info;
                })
                .collect(Collectors.toList());

            // Construir respuesta completa
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("exito", true);
            respuesta.put("orderId", idPedido);
            respuesta.put("currentStatus", estadoGeneral);
            respuesta.put("isInFlight", isInFlight);
            respuesta.put("currentFlightInfo", currentFlightInfo);
            respuesta.put("pedido", Map.of(
                "id", pedido.getId(),
                "fechaPedido", pedido.getFechaPedido() != null ? pedido.getFechaPedido().toString() : null,
                "fechaLimiteEntrega", pedido.getFechaLimiteEntrega() != null ? pedido.getFechaLimiteEntrega().toString() : null,
                "estado", pedido.getEstado() != null ? pedido.getEstado().toString() : null,
                "origenIATA", pedido.getAeropuertoOrigenCodigo() != null ? pedido.getAeropuertoOrigenCodigo() : null,
                "destinoIATA", pedido.getAeropuertoDestinoCodigo() != null ? pedido.getAeropuertoDestinoCodigo() : null
            ));
            respuesta.put("cantidadProductos", productosDelPedido.size());
            respuesta.put("productos", productosConUbicacion);

            // Calcular estadísticas de entrega
            long productosEntregados = productosDelPedido.stream()
                .filter(p -> p.getEstado() != null && p.getEstado().name().equals("ENTREGADO"))
                .count();
            respuesta.put("productosEntregados", productosEntregados);
            respuesta.put("porcentajeEntrega", productosDelPedido.size() > 0 ?
                (double) productosEntregados / productosDelPedido.size() * 100 : 0.0);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, Object> respuestaError = new HashMap<>();
            respuestaError.put("exito", false);
            respuestaError.put("mensaje", "Error al obtener tracking: " + e.getMessage());
            return ResponseEntity.internalServerError().body(respuestaError);
        }
    }

    /**
     * Determina el estado general del pedido basado en sus productos.
     */
    private String determinarEstadoGeneral(List<Producto> productos) {
        if (productos.isEmpty()) {
            return "SIN_PRODUCTOS";
        }

        long enVuelo = productos.stream()
            .filter(p -> p.getEstado() != null && p.getEstado().name().equals("EN_VUELO"))
            .count();
        long entregados = productos.stream()
            .filter(p -> p.getEstado() != null && p.getEstado().name().equals("ENTREGADO"))
            .count();
        long enAlmacen = productos.stream()
            .filter(p -> p.getEstado() != null && p.getEstado().name().equals("EN_ALMACEN"))
            .count();

        // Si todos están entregados
        if (entregados == productos.size()) {
            return "ENTREGADO";
        }
        // Si hay alguno en vuelo
        if (enVuelo > 0) {
            return "EN_VUELO";
        }
        // Si todos están en almacén
        if (enAlmacen == productos.size()) {
            return "EN_ALMACEN";
        }
        // Estado mixto
        return "EN_TRANSITO";
    }

    /**
     * Obtener detalle completo de un pedido para el panel de operadores.
     * Incluye información del cliente, productos con estado, y métricas de urgencia.
     *
     * @param idPedido ID del pedido
     * @return Información completa del pedido para operadores
     *
     * Ejemplo: GET /api/consultas/pedidos/12345/detalle-completo
     */
    @GetMapping("/pedidos/{idPedido}/detalle-completo")
    @Operation(
        summary = "Obtener detalle completo de un pedido",
        description = "Retorna información completa del pedido incluyendo cliente, productos y métricas de urgencia. " +
                      "Diseñado para el panel de detalles de operadores."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Detalle obtenido exitosamente"),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error al obtener detalle")
    })
    public ResponseEntity<Map<String, Object>> obtenerDetalleCompleto(
            @Parameter(description = "ID del pedido", example = "12345")
            @PathVariable Integer idPedido) {

        try {
            // Obtener pedido
            Pedido pedido = pedidoService.buscarPorId(idPedido);

            if (pedido == null) {
                Map<String, Object> respuestaError = new HashMap<>();
                respuestaError.put("exito", false);
                respuestaError.put("mensaje", "Pedido no encontrado: " + idPedido);
                return ResponseEntity.status(404).body(respuestaError);
            }

            // Obtener productos del pedido
            List<Producto> productosDelPedido = productoService.buscarPorPedido(idPedido);

            // Construir información del pedido
            Map<String, Object> pedidoInfo = new HashMap<>();
            pedidoInfo.put("id", pedido.getId());
            pedidoInfo.put("externalId", pedido.getExternalId());
            pedidoInfo.put("nombre", pedido.getNombre());
            pedidoInfo.put("estado", pedido.getEstado() != null ? pedido.getEstado().toString() : "DESCONOCIDO");
            pedidoInfo.put("prioridad", pedido.getPrioridad());
            pedidoInfo.put("origenIATA", pedido.getAeropuertoOrigenCodigo());
            pedidoInfo.put("destinoIATA", pedido.getAeropuertoDestinoCodigo());
            pedidoInfo.put("fechaPedido", pedido.getFechaPedido() != null ? pedido.getFechaPedido().toString() : null);
            pedidoInfo.put("fechaLimiteEntrega", pedido.getFechaLimiteEntrega() != null ? pedido.getFechaLimiteEntrega().toString() : null);
            pedidoInfo.put("horasRecogida", pedido.getHorasRecogida());
            pedidoInfo.put("cantidadProductos", pedido.getCantidadProductos());

            // Construir información del cliente (si existe)
            Map<String, Object> clienteInfo = null;
            if (pedido.getCliente() != null) {
                var cliente = pedido.getCliente();
                clienteInfo = new HashMap<>();
                String nombreCompleto = "";
                if (cliente.getNombres() != null) {
                    nombreCompleto = cliente.getNombres();
                }
                if (cliente.getApellidos() != null) {
                    nombreCompleto += " " + cliente.getApellidos();
                }
                clienteInfo.put("nombreCompleto", nombreCompleto.trim());
                clienteInfo.put("tipoDocumento", cliente.getTipoDocumento() != null ? cliente.getTipoDocumento().toString() : null);
                clienteInfo.put("numeroDocumento", cliente.getNumeroDocumento());
                clienteInfo.put("correo", cliente.getCorreo());
                clienteInfo.put("telefono", cliente.getTelefono());
                clienteInfo.put("ciudadRecojo", cliente.getCiudadRecojo() != null ? cliente.getCiudadRecojo().getNombre() : null);
            }

            // Construir lista de productos con información completa
            List<Map<String, Object>> productosInfo = productosDelPedido.stream()
                .map(p -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("id", p.getId());
                    info.put("nombre", p.getNombre());
                    info.put("peso", p.getPeso());
                    info.put("volumen", p.getVolumen());
                    info.put("estado", p.getEstado() != null ? p.getEstado().toString() : "DESCONOCIDO");
                    info.put("instanciaVuelo", p.getInstanciaVueloAsignada());
                    info.put("fechaLlegada", p.getFechaHoraLlegada() != null ? p.getFechaHoraLlegada().toString() : null);
                    return info;
                })
                .collect(Collectors.toList());

            // Métricas - el tiempo restante se calcula en el frontend usando el tiempo simulado
            // El backend solo proporciona los datos estáticos
            Map<String, Object> metricas = new HashMap<>();
            // NOTA: tiempoRestanteMinutos y esUrgente se calculan en el frontend
            // usando el tiempo simulado actual (no el tiempo real del sistema)
            metricas.put("tiempoRestanteMinutos", 0); // Placeholder - frontend lo calcula
            metricas.put("esUrgente", false); // Placeholder - frontend lo calcula
            
            // Calcular porcentaje de entrega
            long productosEntregados = productosDelPedido.stream()
                .filter(p -> p.getEstado() != null && p.getEstado().name().equals("ENTREGADO"))
                .count();
            double porcentajeEntrega = productosDelPedido.size() > 0 
                ? (double) productosEntregados / productosDelPedido.size() * 100 
                : 0.0;
            metricas.put("porcentajeEntrega", porcentajeEntrega);
            metricas.put("productosEntregados", productosEntregados);
            metricas.put("productosEnVuelo", productosDelPedido.stream()
                .filter(p -> p.getEstado() != null && p.getEstado().name().equals("EN_VUELO"))
                .count());
            metricas.put("productosEnAlmacen", productosDelPedido.stream()
                .filter(p -> p.getEstado() != null && p.getEstado().name().equals("EN_ALMACEN"))
                .count());

            // Construir respuesta completa
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("exito", true);
            respuesta.put("pedido", pedidoInfo);
            respuesta.put("cliente", clienteInfo);
            respuesta.put("productos", productosInfo);
            respuesta.put("metricas", metricas);

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, Object> respuestaError = new HashMap<>();
            respuestaError.put("exito", false);
            respuestaError.put("mensaje", "Error al obtener detalle del pedido: " + e.getMessage());
            return ResponseEntity.internalServerError().body(respuestaError);
        }
    }

    /**
     * Obtiene información del vuelo actual para productos en vuelo.
     */
    private Map<String, Object> obtenerInfoVueloActual(List<Producto> productos) {
        // Encontrar el primer producto que está en vuelo con instancia asignada
        Producto productoEnVuelo = productos.stream()
            .filter(p -> p.getEstado() != null &&
                        p.getEstado().name().equals("EN_VUELO") &&
                        p.getInstanciaVueloAsignada() != null)
            .findFirst()
            .orElse(null);

        if (productoEnVuelo == null) {
            return null;
        }

        String instanciaVuelo = productoEnVuelo.getInstanciaVueloAsignada();
        // Formato: "FL-{vueloId}-DAY-{day}-{HHmm}"
        // Ejemplo: "FL-45-DAY-0-0800"

        Map<String, Object> flightInfo = new HashMap<>();
        flightInfo.put("eventId", instanciaVuelo); // Usado para encontrar el marcador en el mapa

        // Intentar extraer el vueloId de la instancia
        if (instanciaVuelo.startsWith("FL-")) {
            try {
                String[] parts = instanciaVuelo.split("-");
                if (parts.length >= 2) {
                    int vueloId = Integer.parseInt(parts[1]);
                    flightInfo.put("flightId", vueloId);

                    // Buscar el vuelo para obtener más información
                    List<Vuelo> vuelos = vueloService.listar();
                    Vuelo vuelo = vuelos.stream()
                        .filter(v -> v.getId().equals(vueloId))
                        .findFirst()
                        .orElse(null);

                    if (vuelo != null) {
                        flightInfo.put("flightCode", vuelo.getIdentificadorVuelo());
                        if (vuelo.getAeropuertoOrigen() != null) {
                            flightInfo.put("originCode", vuelo.getAeropuertoOrigen().getCodigoIATA());
                        }
                        if (vuelo.getAeropuertoDestino() != null) {
                            flightInfo.put("destinationCode", vuelo.getAeropuertoDestino().getCodigoIATA());
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // Si no podemos parsear, retornar solo eventId
            }
        }

        return flightInfo;
    }

    /**
     * Obtener detalles de un vuelo por código.
     *
     * @param codigoVuelo Código del vuelo (ej: "LIMA-BRUS-001")
     * @return Detalles del vuelo con uso de capacidad
     *
     * Ejemplo: GET /api/consultas/vuelos/LIMA-BRUS-001
     */
    @GetMapping("/vuelos/{codigoVuelo}")
    @Operation(
        summary = "Obtener detalles de un vuelo",
        description = "Retorna detalles de un vuelo específico con información de capacidad"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Vuelo obtenido exitosamente"),
        @ApiResponse(responseCode = "404", description = "Vuelo no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error al obtener vuelo")
    })
    public ResponseEntity<Map<String, Object>> obtenerDetallesVuelo(
            @Parameter(description = "Código del vuelo", example = "LIMA-BRUS-001")
            @PathVariable String codigoVuelo) {

        try {
            // Buscar vuelo por identificador
            List<Vuelo> todosVuelos = vueloService.listar();
            Vuelo vuelo = todosVuelos.stream()
                .filter(v -> codigoVuelo.equals(v.getIdentificadorVuelo()))
                .findFirst()
                .orElse(null);

            if (vuelo == null) {
                Map<String, Object> respuestaError = new HashMap<>();
                respuestaError.put("exito", false);
                respuestaError.put("mensaje", "Vuelo no encontrado: " + codigoVuelo);
                return ResponseEntity.notFound().build();
            }

            // Construir respuesta
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("exito", true);
            respuesta.put("vuelo", vuelo);
            respuesta.put("capacidadUsada", vuelo.getCapacidadUsada());
            respuesta.put("capacidadTotal", vuelo.getCapacidadMaxima());
            respuesta.put("capacidadDisponible", Math.max(0, vuelo.getCapacidadMaxima() - vuelo.getCapacidadUsada()));

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            Map<String, Object> respuestaError = new HashMap<>();
            respuestaError.put("exito", false);
            respuestaError.put("mensaje", "Error al obtener detalles del vuelo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(respuestaError);
        }
    }
}

