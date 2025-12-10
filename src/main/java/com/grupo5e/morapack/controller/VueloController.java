package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.BulkResponseDTO;
import com.grupo5e.morapack.api.dto.ErrorResponseDTO;
import com.grupo5e.morapack.api.dto.ResultadoCancelacionDTO;
import com.grupo5e.morapack.api.dto.VueloDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.api.mapper.VueloMapper;
import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.enums.EstadoVuelo;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.repository.InstanciaVueloRepository;
import com.grupo5e.morapack.repository.PlanViajeRepository;
import com.grupo5e.morapack.repository.ProductoRepository;
import com.grupo5e.morapack.repository.SegmentoVueloRepository;
import com.grupo5e.morapack.service.CancelacionService;
import com.grupo5e.morapack.service.VueloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vuelos")
@Tag(name = "Vuelos", description = "API para gestión de vuelos")
@Slf4j
public class VueloController {

    private final VueloService vueloService;
    private final VueloMapper vueloMapper;
    private final ProductoRepository productoRepository;
    private final InstanciaVueloRepository instanciaVueloRepository;
    private final CancelacionService cancelacionService;
    private final SegmentoVueloRepository segmentoVueloRepository;
    private final PlanViajeRepository planViajeRepository;

    public VueloController(VueloService vueloService, VueloMapper vueloMapper,
                           ProductoRepository productoRepository,
                           InstanciaVueloRepository instanciaVueloRepository,
                           CancelacionService cancelacionService, SegmentoVueloRepository segmentoVueloRepository,
                           PlanViajeRepository planViajeRepository) {
        this.vueloService = vueloService;
        this.vueloMapper = vueloMapper;
        this.productoRepository = productoRepository;
        this.instanciaVueloRepository = instanciaVueloRepository;
        this.cancelacionService = cancelacionService;
        this.segmentoVueloRepository = segmentoVueloRepository;
        this.planViajeRepository = planViajeRepository;
    }
    /**
     * Cancela una instancia de vuelo concreta (FL-...-yyyyMMdd-HHmm)
     * usando SegmentoVuelo para determinar los pedidos y productos afectados.
     */
    @Operation(
            summary = "Cancelar instancia de vuelo y reinsertar productos",
            description = "Cancela una instancia específica de vuelo (no el vuelo base) siempre que aún no haya despegado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado de la cancelación",
                    content = @Content(schema = @Schema(implementation = ResultadoCancelacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Instancia no encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @PostMapping("/instancias/{idInstancia}/cancelar-y-reasignar")
    public ResponseEntity<ResultadoCancelacionDTO> cancelarInstanciaDeVuelo(
            @PathVariable String idInstancia,
            @RequestParam("tiempoSimulacionActual")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime tiempoSimulacionActual
    ) {
        log.info("✈️ [CANCELAR] Solicitud para cancelar instancia {} en tiempoSimulacionActual={}",
                idInstancia, tiempoSimulacionActual);

        // --------------------------------------------------------------------
        // 0) Buscar instancia
        // --------------------------------------------------------------------
        InstanciaVuelo instancia = instanciaVueloRepository.findById(idInstancia)
                .orElse(null);
        Vuelo vueloBase=instancia.getVueloBase();
        if (instancia == null) {
            log.warn("❌ [CANCELAR] Instancia {} no encontrada", idInstancia);
            ResultadoCancelacionDTO dto = ResultadoCancelacionDTO.builder()
                    .exitoso(false)
                    .mensaje("Instancia de vuelo no encontrada")
                    .idInstancia(idInstancia)
                    .build();
            return ResponseEntity.ok(dto);
        }

        log.info("✈️ [CANCELAR] Instancia encontrada: vueloBaseId={}, salida={}, estado={}",
                instancia.getVueloBase().getId(),
                instancia.getFechaHoraSalida(),
                instancia.getEstadoInstancia());

        // --------------------------------------------------------------------
        // 1) Validar estado actual de la instancia
        // --------------------------------------------------------------------
        if ("CANCELADA".equalsIgnoreCase(instancia.getEstadoInstancia())) {
            log.info("ℹ️ [CANCELAR] Instancia {} ya estaba CANCELADA", idInstancia);
            ResultadoCancelacionDTO dto = ResultadoCancelacionDTO.builder()
                    .exitoso(false)
                    .mensaje("La instancia ya estaba cancelada")
                    .idInstancia(idInstancia)
                    .vueloBaseId(instancia.getVueloBase().getId())
                    .build();
            return ResponseEntity.ok(dto);
        }

        if ("FINALIZADA".equalsIgnoreCase(instancia.getEstadoInstancia())) {
            log.info("ℹ️ [CANCELAR] Instancia {} ya está FINALIZADA, no se puede cancelar", idInstancia);
            ResultadoCancelacionDTO dto = ResultadoCancelacionDTO.builder()
                    .exitoso(false)
                    .mensaje("El vuelo ya fue finalizado, no se puede cancelar")
                    .idInstancia(idInstancia)
                    .vueloBaseId(instancia.getVueloBase().getId())
                    .build();
            return ResponseEntity.ok(dto);
        }

        // --------------------------------------------------------------------
        // 2) Validar que aún no haya despegado
        // --------------------------------------------------------------------
        // Ajustar tiempoSimulacionActual restando 5 horas (UTC-5)
        //LocalDateTime tiempoSimulacionAjustado = tiempoSimulacionActual.minusHours(5);

        log.info("⏱️ [CANCELAR] tiempoSimulacionActual={} vs salidaInstancia={}",
                tiempoSimulacionActual, instancia.getFechaHoraSalida());

        if (!tiempoSimulacionActual.isBefore(instancia.getFechaHoraSalida())) {
            // tiempoSimulacionActual >= fechaHoraSalida
            log.warn("❌ [CANCELAR] El vuelo ya despegó según tiempo de simulación, no se cancela.");
            ResultadoCancelacionDTO dto = ResultadoCancelacionDTO.builder()
                    .exitoso(false)
                    .mensaje("El vuelo ya despegó, no puede ser cancelado.")
                    .idInstancia(idInstancia)
                    .vueloBaseId(instancia.getVueloBase().getId())
                    .build();
            return ResponseEntity.ok(dto);
        }

        // --------------------------------------------------------------------
        // 3) Buscar los segmentos que usan ESTA instancia
        //     (misma vueloBase + misma fechaHoraSalida)
        // --------------------------------------------------------------------
        List<SegmentoVuelo> segmentosDeInstancia =
                segmentoVueloRepository.findByVueloAndHoraSalidaExacta(
                        instancia.getVueloBase().getId(),
                        instancia.getFechaHoraSalida()
                );

        if (segmentosDeInstancia.isEmpty()) {
            log.info("ℹ️ [CANCELAR] No hay SegmentoVuelo asociados a la instancia {}, nada que replanificar.",
                    idInstancia);

            instancia.setEstadoInstancia("CANCELADA");
            instanciaVueloRepository.save(instancia);

            ResultadoCancelacionDTO dto = ResultadoCancelacionDTO.builder()
                    .exitoso(true)
                    .mensaje("Instancia cancelada, pero no había segmentos asociados.")
                    .idInstancia(idInstancia)
                    .vueloBaseId(instancia.getVueloBase().getId())
                    .build();
            return ResponseEntity.ok(dto);
        }

        log.info("✈️ [CANCELAR] Segmentos afectados en esta instancia: {}",
                segmentosDeInstancia.size());

        // --------------------------------------------------------------------
        // 4) Reunir los PlanViaje afectados
        // --------------------------------------------------------------------
        Set<PlanViaje> planesAfectados = new HashSet<>();
        for (SegmentoVuelo seg : segmentosDeInstancia) {
            PlanViaje plan = seg.getPlanViaje();
            if (plan != null) {
                planesAfectados.add(plan);
            }
        }

        log.info("✈️ [CANCELAR] Planes de viaje afectados: {}", planesAfectados.size());

        // Cache local de instancias que vamos modificando
        //  clave: vueloId|fechaHoraSalida
        Map<String, InstanciaVuelo> instanciasModificadas = new HashMap<>();

        int totalCapacidadLiberada = 0;
        int totalSegmentosAfectados = 0;

        // --------------------------------------------------------------------
        // 5) Para cada PlanViaje afectado:
        //    - liberamos capacidad en TODAS las instancias de SUS segmentos
        //      (incluyendo escalas)
        //    - marcamos el PlanViaje como CANCELADO
        // --------------------------------------------------------------------
        for (PlanViaje plan : planesAfectados) {
            if (plan.getSegmentosVuelo() == null || plan.getSegmentosVuelo().isEmpty()) {
                continue;
            }

            log.info("🧭 [CANCELAR] Procesando PlanViaje id={} con {} segmentos",
                    plan.getId(), plan.getSegmentosVuelo().size());

            for (SegmentoVuelo segPlan : plan.getSegmentosVuelo()) {
                if (segPlan.getVuelo() == null || segPlan.getHoraSalidaEstimada() == null) {
                    continue;
                }

                // ✅ VALIDACIÓN CRÍTICA: Solo liberar capacidad de segmentos POSTERIORES o IGUALES
                // a la instancia cancelada
                if (segPlan.getHoraSalidaEstimada().isBefore(instancia.getFechaHoraSalida())) {
                    log.info("⏭️ [CANCELAR]   Segmento segId={} con salida {} es ANTERIOR a la instancia a cancelar {}. NO se libera capacidad.",
                            segPlan.getId(),
                            segPlan.getHoraSalidaEstimada(),
                            instancia.getFechaHoraSalida());
                    continue;
                }

                if (segPlan.getPedido() == null || segPlan.getPedido().getCantidadProductos() == 0) {
                    log.warn("⚠️ [CANCELAR]   Segmento segId={} sin pedido o sin cantidadProductos, se omite",
                            segPlan.getId());
                    continue;
                }

                int cantidadPedido = segPlan.getPedido().getCantidadProductos();
                if (cantidadPedido <= 0) {
                    log.info("ℹ️ [CANCELAR]   Segmento segId={} tiene cantidadProductos={} (<=0), no libera capacidad",
                            segPlan.getId(), cantidadPedido);
                    continue;
                }
                Integer vueloId = segPlan.getVuelo().getId();
                LocalDateTime salidaSegmento = segPlan.getHoraSalidaEstimada();

                // clave de cache local
                String cacheKey = vueloId + "|" + salidaSegmento.toString();


                InstanciaVuelo instanciaSeg = instanciasModificadas.computeIfAbsent(
                        cacheKey,
                        k -> {
                            InstanciaVuelo inst = instanciaVueloRepository
                                    .findByVueloBase_IdAndFechaHoraSalida(vueloId, salidaSegmento)
                                    .orElse(null);
                            log.info("📦 [CANCELAR]   Cacheando instancia para key={} vueloId={} salida={} -> encontrada={}",
                                    k, vueloId, salidaSegmento, (inst != null));
                            return inst;
                        }
                );

                if (instanciaSeg == null) {
                    log.warn("⚠️ [CANCELAR]   No se encontró InstanciaVuelo para vueloId={} salida={}, segmento segId={}",
                            vueloId, salidaSegmento, segPlan.getId());
                    continue;
                }

                Integer usadosActual = Optional.ofNullable(instanciaSeg.getCapacidadUsada()).orElse(0);
                Integer prodActual = Optional.ofNullable(instanciaSeg.getProductosAsignados()).orElse(0);

                int nuevosUsados = usadosActual - segPlan.getCapacidadReservada();
                if (nuevosUsados < 0) {
                    log.warn("⚠️ [CANCELAR]   Capacidad negativa en instancia {}. usadosActual={} liberar={} -> ajustando a 0",
                            instanciaSeg.getIdInstancia(), usadosActual, cantidadPedido);
                    nuevosUsados = 0;
                }

                log.info("📊 [CANCELAR]   Instancia {} ANTES: usados={}, productosAsignados={}. Liberando {} unidades.",
                        instanciaSeg.getIdInstancia(), usadosActual, prodActual, cantidadPedido);

                instanciaSeg.setCapacidadUsada(nuevosUsados);
                instanciaSeg.setProductosAsignados(nuevosUsados);

                log.info("📊 [CANCELAR]   Instancia {} DESPUÉS: usados={}, productosAsignados={}",
                        instanciaSeg.getIdInstancia(),
                        instanciaSeg.getCapacidadUsada(),
                        instanciaSeg.getProductosAsignados());
                    totalCapacidadLiberada += segPlan.getPedido().getCantidadProductos();
                    totalSegmentosAfectados++;
                //}
            }

            // Toda la ruta de ese pedido/producto queda inválida
            plan.setEstado("CANCELADO");
        }

        // --------------------------------------------------------------------
        // 6) Marcar la instancia directamente cancelada
        // --------------------------------------------------------------------
        instancia.setEstadoInstancia("CANCELADO");
        String keyInstanciaPrincipal = instancia.getVueloBase().getId() + "|" + instancia.getFechaHoraSalida();
        instanciasModificadas.put(keyInstanciaPrincipal, instancia);

        // --------------------------------------------------------------------
        // 7) Persistir cambios
        // --------------------------------------------------------------------
        planViajeRepository.saveAll(planesAfectados);
        instanciaVueloRepository.save(instancia);
        if (!instanciasModificadas.isEmpty()) {
            log.info("💾 [CANCELAR] Guardando {} instancias de vuelo modificadas",
                    instanciasModificadas.size());
            instanciaVueloRepository.saveAll(instanciasModificadas.values());
        }
        Cancelacion cancelacion = new Cancelacion();
        cancelacion.setDiasCancelado(1);
        cancelacion.setCodigoIATAOrigen(vueloBase.getAeropuertoOrigen().getCodigoIATA());
        cancelacion.setCodigoIATADestino(vueloBase.getAeropuertoDestino().getCodigoIATA());
        cancelacion.setHora(instancia.getFechaHoraSalida().getHour());
        cancelacion.setMinuto(instancia.getFechaHoraSalida().getMinute());
        cancelacion.setFechaHoraCancelacion(tiempoSimulacionActual);
        cancelacion.setVuelo(vueloBase);

        try {
            cancelacionService.insertar(cancelacion);
            log.info("Cancelación registrada en BD: {} -> {} a las {}:{} del día {}",
                    cancelacion.getCodigoIATAOrigen(),
                    cancelacion.getCodigoIATADestino(),
                    cancelacion.getHora(),
                    cancelacion.getMinuto(),
                    cancelacion.getDiasCancelado());
        } catch (Exception e) {
            log.error("Error al registrar cancelación en BD: {}", e.getMessage());
        }
        log.info("✅ [CANCELAR] Instancia {} CANCELADA. Segmentos afectados={}, capacidad liberada={}",
                idInstancia, totalSegmentosAfectados, totalCapacidadLiberada);

        // --------------------------------------------------------------------
        // 8) Respuesta
        // --------------------------------------------------------------------
        ResultadoCancelacionDTO dto = ResultadoCancelacionDTO.builder()
                .exitoso(true)
                .mensaje(String.format(
                        "Instancia cancelada. Segmentos afectados: %d, capacidad liberada: %d",
                        totalSegmentosAfectados, totalCapacidadLiberada))
                .idInstancia(instancia.getIdInstancia())
                .vueloBaseId(instancia.getVueloBase().getId())
                .productosAfectados(totalCapacidadLiberada)
                .origen(instancia.getVueloBase().getAeropuertoOrigen().getCodigoIATA())
                .destino(instancia.getVueloBase().getAeropuertoDestino().getCodigoIATA())
                .build();

        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Listar todos los vuelos", description = "Obtiene una lista de todos los vuelos como DTOs (sin lazy loading)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de vuelos obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<VueloDTO>> listar() {
        List<VueloDTO> vuelos = vueloService.listar().stream()
                .map(vueloMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(vuelos);
    }

    @Operation(summary = "Obtener vuelo por ID", description = "Obtiene un vuelo específico por su ID como DTO")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vuelo encontrado"),
            @ApiResponse(responseCode = "404", description = "Vuelo no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<VueloDTO> obtenerPorId(
            @Parameter(description = "ID del vuelo", required = true) @PathVariable Integer id) {
        Vuelo vuelo = vueloService.buscarPorId(id);
        if (vuelo == null) {
            throw new ResourceNotFoundException("Vuelo", "id", id);
        }
        return ResponseEntity.ok(vueloMapper.toDTO(vuelo));
    }

    @Operation(summary = "Buscar vuelos por ruta", description = "Busca vuelos entre dos aeropuertos específicos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vuelos encontrados")
    })
    @GetMapping("/ruta")
    public ResponseEntity<List<VueloDTO>> buscarPorRuta(
            @Parameter(description = "ID del aeropuerto origen", required = true) @RequestParam Integer origenId,
            @Parameter(description = "ID del aeropuerto destino", required = true) @RequestParam Integer destinoId) {
        List<VueloDTO> vuelos = vueloService.buscarPorRuta(origenId, destinoId).stream()
                .map(vueloMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(vuelos);
    }

    @Operation(summary = "Obtener vuelos por estado", description = "Obtiene todos los vuelos con un estado específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de vuelos con el estado especificado")
    })
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Vuelo>> obtenerPorEstado(
            @Parameter(description = "Estado del vuelo", required = true) @PathVariable EstadoVuelo estado) {
        return ResponseEntity.ok(vueloService.buscarPorEstado(estado));
    }

    @Operation(summary = "Obtener vuelos disponibles", description = "Obtiene vuelos con capacidad disponible")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de vuelos disponibles")
    })
    @GetMapping("/disponibles")
    public ResponseEntity<List<Vuelo>> obtenerDisponibles(
            @Parameter(description = "Capacidad mínima requerida", required = false) @RequestParam(defaultValue = "1") int capacidadMinima) {
        return ResponseEntity.ok(vueloService.buscarDisponibles(capacidadMinima));
    }

    @Operation(summary = "Buscar vuelo por identificador", description = "Busca un vuelo por su identificador único (ORIGEN-DESTINO-HH:MM)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vuelo encontrado"),
            @ApiResponse(responseCode = "404", description = "Vuelo no encontrado")
    })
    @GetMapping("/identificador/{identificador}")
    public ResponseEntity<Vuelo> buscarPorIdentificador(
            @Parameter(description = "Identificador del vuelo (ej: SKBO-SEQM-08:30)", required = true) @PathVariable String identificador) {
        Optional<Vuelo> vuelo = vueloService.buscarPorIdentificador(identificador);
        return vuelo.map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Vuelo", "identificador", identificador));
    }

    @Operation(summary = "Crear nuevo vuelo", description = "Registra un nuevo vuelo en el sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vuelo creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Integer> crear(@Valid @RequestBody Vuelo vuelo) {
        int id = vueloService.insertar(vuelo);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @Operation(summary = "Actualizar vuelo", description = "Actualiza los datos de un vuelo existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vuelo actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Vuelo no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Vuelo> actualizar(
            @Parameter(description = "ID del vuelo", required = true) @PathVariable Integer id,
            @Valid @RequestBody Vuelo vuelo) {
        Vuelo actualizado = vueloService.actualizar(id, vuelo);
        return ResponseEntity.ok(actualizado);
    }

    @Operation(summary = "Eliminar vuelo", description = "Elimina un vuelo del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vuelo eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Vuelo no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del vuelo", required = true) @PathVariable Integer id) {
        vueloService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Crear vuelos en bulk", description = "Registra múltiples vuelos en una sola operación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Vuelos creados exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error en validación de datos")
    })
    @PostMapping("/bulk")
    public ResponseEntity<BulkResponseDTO<Integer>> crearBulk(@Valid @RequestBody List<Vuelo> vuelos) {
        List<Vuelo> creados = vueloService.insertarBulk(vuelos);
        List<Integer> ids = creados.stream().map(Vuelo::getId).collect(Collectors.toList());

        BulkResponseDTO<Integer> response = BulkResponseDTO.<Integer>builder()
                .totalProcesados(vuelos.size())
                .exitosos(ids.size())
                .fallidos(0)
                .idsExitosos(ids)
                .mensaje("Vuelos creados exitosamente")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint para cancelar un vuelo y re-asignar productos afectados.
     * 
     * VALIDACIÓN CRÍTICA:
     * - El vuelo solo puede cancelarse si NO ha despegado aún
     * - Se verifica que no haya productos en estado IN_TRANSIT
     * - Se intenta re-asignar automáticamente los productos afectados
     * - Si no se pueden re-asignar, se marca que requiere re-optimización
     * 
     * @param id                     ID del vuelo a cancelar
     * @param tiempoSimulacionActual Tiempo actual de la simulación
     * @return Resultado de la cancelación con estadísticas
     */
    @Operation(summary = "Cancelar vuelo y re-asignar productos", description = "Cancela un vuelo solo si no ha despegado aún. Intenta re-asignar productos afectados automáticamente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vuelo cancelado exitosamente"),
            @ApiResponse(responseCode = "409", description = "El vuelo no puede cancelarse - ya despegó", content = @Content(schema = @Schema(implementation = ResultadoCancelacionDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vuelo no encontrado")
    })
    @PostMapping("/{id}/cancelar-y-reasignar")
    public ResponseEntity<ResultadoCancelacionDTO> cancelarYReasignar(
            @Parameter(description = "ID del vuelo", required = true) @PathVariable Integer id,
            @Parameter(description = "Tiempo actual de simulación", required = true) @RequestParam LocalDateTime tiempoSimulacionActual) {

        log.info("========================================");
        log.info("SOLICITUD DE CANCELACIÓN DE VUELO");
        log.info("Vuelo ID: {}", id);
        log.info("Tiempo actual: {}", tiempoSimulacionActual);
        log.info("========================================");

        // VALIDACIÓN CRÍTICA: Verificar si el vuelo puede cancelarse
        if (!vueloService.puedeSerCancelado(id, tiempoSimulacionActual)) {
            log.warn("✗ El vuelo {} no puede cancelarse - ya despegó o tiene productos en tránsito", id);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ResultadoCancelacionDTO.builder()
                            .exitoso(false)
                            .mensaje("El vuelo no puede cancelarse - ya despegó o tiene productos en tránsito")
                            .vueloId(id)
                            .build());
        }

        // Obtener el vuelo
        Vuelo vuelo = vueloService.buscarPorId(id);
        if (vuelo == null) {
            throw new ResourceNotFoundException("Vuelo", "id", id);
        }

        // Obtener productos afectados (buscar por patrón de instancia)
        String patronBusqueda = "FL-" + id + "-";
        List<Producto> productosAfectados = productoRepository
                .findByInstanciaVueloAsignadaContaining(patronBusqueda);

        log.info("Productos afectados por cancelación: {}", productosAfectados.size());

        // Marcar productos como PENDING (sin asignación)
        List<Producto> productosReasignados = new ArrayList<>();
        List<Producto> productosSinAsignar = new ArrayList<>();

        for (Producto producto : productosAfectados) {
            // Limpiar asignación
            producto.setInstanciaVueloAsignada(null);

            // Usar EN_ALMACEN como estado inicial (equivalente a PENDING)
            producto.setEstado(EstadoProducto.EN_ALMACEN);

            productoRepository.save(producto);

            // TODO: Implementar lógica de re-asignación automática
            // Por ahora, todos los productos quedan sin asignar
            productosSinAsignar.add(producto);
        }

        // Marcar vuelo como FINALIZADO (como cancelado/no disponible)
        vuelo.setEstado(EstadoVuelo.FINALIZADO);
        vueloService.actualizar(id, vuelo);

        log.info("========================================");
        log.info("✅ CANCELACIÓN COMPLETADA");
        log.info("Productos afectados: {}", productosAfectados.size());
        log.info("Productos re-asignados automáticamente: {}", productosReasignados.size());
        log.info("Productos sin asignar (requieren re-optimización): {}", productosSinAsignar.size());
        log.info("========================================\n");

        boolean requiereReoptimizacion = !productosSinAsignar.isEmpty();

        return ResponseEntity.ok(ResultadoCancelacionDTO.builder()
                .exitoso(true)
                .vueloId(id)
                .productosAfectados(productosAfectados.size())
                .productosReasignados(productosReasignados.size())
                .productosSinAsignar(productosSinAsignar.size())
                .requiereReoptimizacion(requiereReoptimizacion)
                .mensaje(String.format(
                        "Vuelo cancelado exitosamente. %d productos afectados, %d requieren re-optimización",
                        productosAfectados.size(),
                        productosSinAsignar.size()))
                .build());
    }
}
