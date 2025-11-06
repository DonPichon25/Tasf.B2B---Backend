package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.algorithm.alns.ALNSSolver;
import com.grupo5e.morapack.api.dto.*;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.model.Vuelo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller para ejecución del algoritmo ALNS.
 * Siguiendo patrón 100% de MoraPack-Backend: ejecuta y retorna resultados inmediatamente.
 * NO persiste en base de datos, todo en memoria.
 */
@RestController
@RequestMapping("/api/algoritmo")
@Tag(name = "Algoritmo ALNS", description = "API para ejecución del algoritmo ALNS con resultados a nivel de producto")
@Slf4j
@CrossOrigin(origins = "*")
public class AlgoritmoController {
    
    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;
    
    @Operation(
        summary = "Ejecutar algoritmo ALNS",
        description = "Ejecuta el algoritmo ALNS y retorna resultados a nivel de producto en memoria. " +
                      "No persiste datos - siguiendo patrón Backend"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Algoritmo ejecutado exitosamente",
            content = @Content(schema = @Schema(implementation = ResultadoAlgoritmoDTO.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error durante la ejecución del algoritmo"
        )
    })
    @PostMapping("/ejecutar")
    public ResponseEntity<ResultadoAlgoritmoDTO> ejecutarAlgoritmo(
            @RequestBody(required = false) AlnsRequestDTO solicitud) {
        
        LocalDateTime horaInicio = LocalDateTime.now();
        log.info("🚀 Ejecutando algoritmo ALNS (sincrónico) - Inicio: {}", horaInicio);
        
        try {
            // 1. Configurar fuente de datos según solicitud
            String fuente = (solicitud != null && solicitud.getFuente() != null) 
                ? solicitud.getFuente() : "BASE_DE_DATOS";
            
            log.info("   Fuente de datos solicitada: {}", fuente);
            
            // Configurar variable de entorno para la fuente de datos
            System.setProperty("MODO_FUENTE_DATOS", fuente);
            
            // 2. Crear instancia de ALNSSolver con configuración
            int iteraciones = (solicitud != null && solicitud.getIteraciones() != null) 
                ? solicitud.getIteraciones() : 500;
            
            // Pasar ApplicationContext para que pueda usar servicios de Spring cuando fuente=BASE_DE_DATOS
            com.grupo5e.morapack.algorithm.input.FabricaFuenteDatos.setSpringContext(applicationContext);
            ALNSSolver solver = new ALNSSolver(iteraciones);
            
            // 3. Ejecutar el algoritmo
            log.info("   Iteraciones configuradas: {}", iteraciones);
            solver.resolver();
            
            LocalDateTime horaFin = LocalDateTime.now();
            long segundosEjecucion = ChronoUnit.SECONDS.between(horaInicio, horaFin);
            
            // 3. Obtener solución a nivel de producto
            Map<Producto, ArrayList<Vuelo>> solucionProductos = solver.obtenerSolucionNivelProducto();
            
            // 4. Convertir a DTO
            ResultadoAlgoritmoDTO resultado = convertirSolucionAResultado(
                solucionProductos, 
                horaInicio, 
                horaFin, 
                segundosEjecucion
            );
            
            log.info("✅ Algoritmo completado exitosamente en {} segundos", segundosEjecucion);
            log.info("   Total productos asignados: {}", resultado.getTotalProductos());
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            log.error("❌ Error ejecutando algoritmo ALNS", e);
            
            ResultadoAlgoritmoDTO resultadoError = ResultadoAlgoritmoDTO.builder()
                .exitoso(false)
                .mensaje("Error durante la ejecución: " + e.getMessage())
                .horaInicio(horaInicio)
                .horaFin(LocalDateTime.now())
                .segundosEjecucion(ChronoUnit.SECONDS.between(horaInicio, LocalDateTime.now()))
                .totalProductos(0)
                .totalPedidos(0)
                .rutasProductos(new ArrayList<>())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultadoError);
        }
    }
    
    /**
     * Convierte la solución del algoritmo a ResultadoAlgoritmoDTO
     */
    private ResultadoAlgoritmoDTO convertirSolucionAResultado(
            Map<Producto, ArrayList<Vuelo>> solucionProductos,
            LocalDateTime horaInicio,
            LocalDateTime horaFin,
            long segundosEjecucion) {
        
        List<RutaProductoDTO> rutasProductos = new ArrayList<>();
        
        // Convertir cada producto y su ruta a DTO
        for (Map.Entry<Producto, ArrayList<Vuelo>> entry : solucionProductos.entrySet()) {
            Producto producto = entry.getKey();
            ArrayList<Vuelo> vuelos = entry.getValue();
            
            RutaProductoDTO rutaProducto = RutaProductoDTO.builder()
                .idProducto(producto.getId())
                .idPedido(producto.getPedido() != null ? producto.getPedido().getId() : null)
                .nombrePedido(producto.getPedido() != null ? 
                    producto.getPedido().getNombre() : "Pedido-" + producto.getPedido().getId())
                .nombreProducto(producto.getNombre() != null ? 
                    producto.getNombre() : "Producto-" + producto.getId())
                .peso(producto.getPeso())
                .volumen(producto.getVolumen())
                .codigoOrigen(producto.getPedido() != null ? 
                    producto.getPedido().getAeropuertoOrigenCodigo() : null)
                .codigoDestino(producto.getPedido() != null ? 
                    producto.getPedido().getAeropuertoDestinoCodigo() : null)
                .vuelos(convertirVuelosADTO(vuelos))
                .cantidadVuelos(vuelos.size())
                .tiempoTotalHoras(calcularTiempoTotal(vuelos))
                .estado(producto.getEstado() != null ? producto.getEstado().toString() : "DESCONOCIDO")
                .build();
            
            rutasProductos.add(rutaProducto);
        }
        
        // Calcular estadísticas
        int totalProductos = rutasProductos.size();
        int totalPedidos = rutasProductos.stream()
            .map(RutaProductoDTO::getIdPedido)
            .collect(Collectors.toSet())
            .size();
        
        // Generar timeline temporal de simulación
        log.info("Generando timeline de simulación...");
        LineaDeTiempoSimulacionDTO timeline = generarLineaDeTiempoSimulacion(
            rutasProductos, 
            horaInicio
        );
        log.info("Timeline generado con {} eventos", timeline.getEventos().size());
        
        // Calcular costo total de las rutas
        double costoTotal = rutasProductos.stream()
            .mapToDouble(ruta -> ruta.getVuelos().stream()
                .mapToDouble(v -> v.getCosto() != null ? v.getCosto() : 0.0)
                .sum())
            .sum();
        
        return ResultadoAlgoritmoDTO.builder()
            .exitoso(true)
            .mensaje("Algoritmo ejecutado exitosamente")
            .horaInicio(horaInicio)
            .horaFin(horaFin)
            .segundosEjecucion(segundosEjecucion)
            .totalProductos(totalProductos)
            .totalPedidos(totalPedidos)
            .rutasProductos(rutasProductos)
            .costoTotal(costoTotal)
            .porcentajeAsignacion(100.0) // TODO: calcular basado en pedidos totales
            .lineaDeTiempo(timeline)  // Timeline de simulación en memoria
            .build();
    }
    
    /**
     * Convierte lista de vuelos a VueloSimpleDTO evitando referencias circulares
     */
    private List<VueloSimpleDTO> convertirVuelosADTO(ArrayList<Vuelo> vuelos) {
        return vuelos.stream()
            .map(v -> {
                // Generar código de vuelo basado en ruta
                String codigoOrigen = v.getAeropuertoOrigen() != null ? 
                    v.getAeropuertoOrigen().getCodigoIATA() : "???";
                String codigoDestino = v.getAeropuertoDestino() != null ? 
                    v.getAeropuertoDestino().getCodigoIATA() : "???";
                String codigo = "VL" + codigoOrigen + "-" + codigoDestino + "-" + v.getId();
                
                return VueloSimpleDTO.builder()
                    .id(v.getId())
                    .codigo(codigo)
                    .codigoOrigen(codigoOrigen)
                    .codigoDestino(codigoDestino)
                    .horaSalida(v.getHoraSalida())
                    .horaLlegada(v.getHoraLlegada())
                    .tiempoTransporte(v.getTiempoTransporte())
                    .costo(v.getCosto())
                    .capacidadUsada(v.getCapacidadUsada())
                    .capacidadMaxima(v.getCapacidadMaxima())
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Calcula tiempo total de ruta en horas
     */
    private Double calcularTiempoTotal(ArrayList<Vuelo> vuelos) {
        if (vuelos == null || vuelos.isEmpty()) {
            return 0.0;
        }
        
        return vuelos.stream()
            .mapToDouble(Vuelo::getTiempoTransporte)
            .sum();
    }
    
    /**
     * Genera timeline temporal de simulación con eventos de vuelos.
     * OPTIMIZADO: Agrupa vuelos por ruta para evitar duplicados.
     * 
     * @param rutasProductos Rutas de productos
     * @param horaInicio Hora de inicio de la simulación
     * @return Timeline con eventos ordenados temporalmente
     */
    private LineaDeTiempoSimulacionDTO generarLineaDeTiempoSimulacion(
            List<RutaProductoDTO> rutasProductos,
            LocalDateTime horaInicio) {
        
        List<EventoLineaDeTiempoVueloDTO> eventos = new ArrayList<>();
        Set<Integer> aeropuertosSet = new HashSet<>();
        
        // Clase auxiliar para agrupar vuelos por ruta
        class InfoGrupoVuelo {
            VueloSimpleDTO vuelo;
            LocalDateTime horaSalida;
            LocalDateTime horaLlegada;
            List<Integer> idsProductos = new ArrayList<>();
            List<Integer> idsPedidos = new ArrayList<>();
        }
        
        // Agrupar vuelos por ruta (origen-destino-tiempo) para evitar duplicados
        Map<String, InfoGrupoVuelo> gruposVuelos = new HashMap<>();
        
        // Primer paso: Agrupar vuelos por ruta y tiempo
        for (RutaProductoDTO rutaProducto : rutasProductos) {
            LocalDateTime tiempoActualProducto = horaInicio;
            
            for (int i = 0; i < rutaProducto.getVuelos().size(); i++) {
                VueloSimpleDTO vuelo = rutaProducto.getVuelos().get(i);
                
                // Rastrear aeropuertos
                if (vuelo.getCodigoOrigen() != null) {
                    aeropuertosSet.add(vuelo.getId()); // Simplificado
                }
                
                // Calcular tiempos de salida y llegada
                LocalDateTime horaSalidaVuelo;
                LocalDateTime horaLlegadaVuelo;
                
                if (vuelo.getHoraSalida() != null && vuelo.getHoraLlegada() != null) {
                    // Combinar fecha de simulación con hora programada
                    horaSalidaVuelo = tiempoActualProducto.toLocalDate()
                        .atTime(vuelo.getHoraSalida());
                    
                    // Si la hora de salida ya pasó hoy, programar para mañana
                    if (horaSalidaVuelo.isBefore(tiempoActualProducto)) {
                        horaSalidaVuelo = horaSalidaVuelo.plusDays(1);
                    }
                    
                    // Calcular hora de llegada (puede cruzar medianoche)
                    horaLlegadaVuelo = horaSalidaVuelo.toLocalDate()
                        .atTime(vuelo.getHoraLlegada());
                    if (vuelo.getHoraLlegada().isBefore(vuelo.getHoraSalida())) {
                        horaLlegadaVuelo = horaLlegadaVuelo.plusDays(1);
                    }
                } else {
                    // Fallback: usar tiempo de transporte
                    long minutosTransporte = (long) ((vuelo.getTiempoTransporte() != null ?
                        vuelo.getTiempoTransporte() : 1.0) * 60);
                    horaSalidaVuelo = tiempoActualProducto;
                    horaLlegadaVuelo = horaSalidaVuelo.plusMinutes(minutosTransporte);
                }
                
                // Crear clave única para este vuelo (ruta + hora redondeada)
                LocalDateTime horaSalidaRedondeada = horaSalidaVuelo
                    .withMinute(0).withSecond(0).withNano(0);
                String claveVuelo = vuelo.getCodigoOrigen() + "-" +
                                   vuelo.getCodigoDestino() + "-" +
                                   horaSalidaRedondeada.toString();
                
                InfoGrupoVuelo grupoInfo = gruposVuelos.get(claveVuelo);
                if (grupoInfo == null) {
                    grupoInfo = new InfoGrupoVuelo();
                    grupoInfo.vuelo = vuelo;
                    grupoInfo.horaSalida = horaSalidaVuelo;
                    grupoInfo.horaLlegada = horaLlegadaVuelo;
                    gruposVuelos.put(claveVuelo, grupoInfo);
                }
                
                // Agregar este producto al grupo
                grupoInfo.idsProductos.add(rutaProducto.getIdProducto());
                grupoInfo.idsPedidos.add(rutaProducto.getIdPedido());
                
                // Próximo vuelo: esperar 1 hora de layover después de llegada
                tiempoActualProducto = horaLlegadaVuelo.plusMinutes(60);
            }
        }
        
        log.info("Agrupados {} rutas de productos en {} grupos de vuelos únicos",
                 rutasProductos.size(), gruposVuelos.size());
        
        // Segundo paso: Crear eventos para vuelos agrupados
        int contadorEventos = 0;
        for (Map.Entry<String, InfoGrupoVuelo> entry : gruposVuelos.entrySet()) {
            InfoGrupoVuelo grupo = entry.getValue();
            VueloSimpleDTO vuelo = grupo.vuelo;
            
            // Evento de salida
            EventoLineaDeTiempoVueloDTO eventoSalida = EventoLineaDeTiempoVueloDTO.builder()
                .idEvento("DEP-GROUP-" + contadorEventos)
                .tipoEvento("DEPARTURE")
                .horaEvento(grupo.horaSalida)
                .idVuelo(vuelo.getId())
                .codigoVuelo(vuelo.getCodigo() + " (" + grupo.idsProductos.size() + " pkgs)")
                .idProducto(grupo.idsProductos.get(0)) // Producto representativo
                .idPedido(grupo.idsPedidos.get(0))
                .ciudadOrigen(vuelo.getCodigoOrigen())
                .ciudadDestino(vuelo.getCodigoDestino())
                .idAeropuertoOrigen(vuelo.getId()) // Simplificado
                .idAeropuertoDestino(vuelo.getId())
                .tiempoTransporteDias(vuelo.getTiempoTransporte() != null ? 
                    vuelo.getTiempoTransporte() / 24.0 : 0.0)
                .build();
            
            eventos.add(eventoSalida);
            
            // Evento de llegada
            EventoLineaDeTiempoVueloDTO eventoLlegada = EventoLineaDeTiempoVueloDTO.builder()
                .idEvento("ARR-GROUP-" + contadorEventos)
                .tipoEvento("ARRIVAL")
                .horaEvento(grupo.horaLlegada)
                .idVuelo(vuelo.getId())
                .codigoVuelo(vuelo.getCodigo() + " (" + grupo.idsProductos.size() + " pkgs)")
                .idProducto(grupo.idsProductos.get(0))
                .idPedido(grupo.idsPedidos.get(0))
                .ciudadOrigen(vuelo.getCodigoOrigen())
                .ciudadDestino(vuelo.getCodigoDestino())
                .idAeropuertoOrigen(vuelo.getId())
                .idAeropuertoDestino(vuelo.getId())
                .tiempoTransporteDias(vuelo.getTiempoTransporte() != null ? 
                    vuelo.getTiempoTransporte() / 24.0 : 0.0)
                .build();
            
            eventos.add(eventoLlegada);
            contadorEventos++;
        }
        
        // Ordenar eventos por tiempo
        eventos.sort(Comparator.comparing(EventoLineaDeTiempoVueloDTO::getHoraEvento));
        
        // Encontrar tiempo de fin de simulación
        LocalDateTime horaFin = eventos.isEmpty() ? horaInicio :
            eventos.get(eventos.size() - 1).getHoraEvento();
        
        long duracionMinutos = ChronoUnit.MINUTES.between(horaInicio, horaFin);
        
        log.info("Timeline generado: {} eventos, duración {} minutos",
                 eventos.size(), duracionMinutos);
        
        return LineaDeTiempoSimulacionDTO.builder()
            .horaInicioSimulacion(horaInicio)
            .horaFinSimulacion(horaFin)
            .duracionTotalMinutos(duracionMinutos)
            .eventos(eventos)
            .rutasProductos(rutasProductos)
            .totalProductos(rutasProductos.size())
            .totalVuelos(gruposVuelos.size())
            .totalAeropuertos(aeropuertosSet.size())
            .build();
    }
}

