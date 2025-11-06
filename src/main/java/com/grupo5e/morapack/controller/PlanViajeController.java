package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.EstadisticasPlanesDTO;
import com.grupo5e.morapack.api.dto.PlanViajeDTO;
import com.grupo5e.morapack.api.dto.SegmentoVueloDTO;
import com.grupo5e.morapack.service.PlanViajeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestionar planes de viaje.
 * Siguiendo patrón de MoraPack-Backend: TravelPlanController
 */
@Slf4j
@RestController
@RequestMapping("/api/planes-viaje")
@RequiredArgsConstructor
@Tag(name = "Planes de Viaje", description = "API para gestionar planes de viaje y segmentos de vuelo")
public class PlanViajeController {
    
    private final PlanViajeService planViajeService;
    
    /**
     * Crear un nuevo plan de viaje
     */
    @PostMapping
    @Operation(summary = "Crear plan de viaje", description = "Crea un nuevo plan de viaje con sus segmentos de vuelo")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Plan creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    public ResponseEntity<Integer> crearPlanViaje(@RequestBody PlanViajeDTO planViajeDTO) {
        log.info("POST /api/planes-viaje - Crear plan para pedido: {}", planViajeDTO.getPedidoId());
        try {
            Integer id = planViajeService.crearPlanViaje(planViajeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (RuntimeException e) {
            log.error("Error al crear plan de viaje: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Obtener plan de viaje por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener plan por ID", description = "Obtiene un plan de viaje específico con sus segmentos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plan encontrado"),
        @ApiResponse(responseCode = "404", description = "Plan no encontrado")
    })
    public ResponseEntity<PlanViajeDTO> obtenerPorId(
        @Parameter(description = "ID del plan de viaje") @PathVariable Integer id) {
        log.info("GET /api/planes-viaje/{} - Obtener plan", id);
        try {
            PlanViajeDTO plan = planViajeService.obtenerPorId(id);
            return ResponseEntity.ok(plan);
        } catch (RuntimeException e) {
            log.error("Plan no encontrado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtener todos los planes
     */
    @GetMapping
    @Operation(summary = "Listar todos los planes", description = "Obtiene todos los planes de viaje registrados")
    @ApiResponse(responseCode = "200", description = "Lista de planes")
    public ResponseEntity<List<PlanViajeDTO>> obtenerTodos() {
        log.info("GET /api/planes-viaje - Listar todos");
        List<PlanViajeDTO> planes = planViajeService.obtenerTodos();
        return ResponseEntity.ok(planes);
    }
    
    /**
     * Obtener planes por pedido
     */
    @GetMapping("/pedido/{pedidoId}")
    @Operation(summary = "Planes por pedido", description = "Obtiene todos los planes asociados a un pedido")
    @ApiResponse(responseCode = "200", description = "Lista de planes del pedido")
    public ResponseEntity<List<PlanViajeDTO>> obtenerPorPedido(
        @Parameter(description = "ID del pedido") @PathVariable Integer pedidoId) {
        log.info("GET /api/planes-viaje/pedido/{} - Planes de pedido", pedidoId);
        List<PlanViajeDTO> planes = planViajeService.obtenerPorPedidoId(pedidoId);
        return ResponseEntity.ok(planes);
    }
    
    /**
     * Obtener plan más reciente de un pedido
     */
    @GetMapping("/pedido/{pedidoId}/mas-reciente")
    @Operation(summary = "Plan más reciente", description = "Obtiene el plan más reciente de un pedido")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plan más reciente"),
        @ApiResponse(responseCode = "404", description = "No hay planes para este pedido")
    })
    public ResponseEntity<PlanViajeDTO> obtenerMasReciente(
        @Parameter(description = "ID del pedido") @PathVariable Integer pedidoId) {
        log.info("GET /api/planes-viaje/pedido/{}/mas-reciente", pedidoId);
        try {
            PlanViajeDTO plan = planViajeService.obtenerPlanMasRecientePorPedido(pedidoId);
            return ResponseEntity.ok(plan);
        } catch (RuntimeException e) {
            log.error("No se encontró plan: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtener planes por estado
     */
    @GetMapping("/estado/{estado}")
    @Operation(summary = "Planes por estado", description = "Obtiene planes filtrados por estado")
    @ApiResponse(responseCode = "200", description = "Lista de planes con el estado especificado")
    public ResponseEntity<List<PlanViajeDTO>> obtenerPorEstado(
        @Parameter(description = "Estado del plan (PENDIENTE, EN_PROGRESO, COMPLETADO, CANCELADO)") 
        @PathVariable String estado) {
        log.info("GET /api/planes-viaje/estado/{}", estado);
        List<PlanViajeDTO> planes = planViajeService.obtenerPorEstado(estado);
        return ResponseEntity.ok(planes);
    }
    
    /**
     * Actualizar estado de un plan
     */
    @PatchMapping("/{id}/estado")
    @Operation(summary = "Actualizar estado", description = "Cambia el estado de un plan de viaje")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(responseCode = "404", description = "Plan no encontrado")
    })
    public ResponseEntity<Void> actualizarEstado(
        @Parameter(description = "ID del plan") @PathVariable Integer id,
        @Parameter(description = "Nuevo estado") @RequestParam String estado) {
        log.info("PATCH /api/planes-viaje/{}/estado - Actualizar a: {}", id, estado);
        try {
            planViajeService.actualizarEstado(id, estado);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error al actualizar estado: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Eliminar plan de viaje
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar plan", description = "Elimina un plan de viaje y sus segmentos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Plan eliminado"),
        @ApiResponse(responseCode = "404", description = "Plan no encontrado")
    })
    public ResponseEntity<Void> eliminar(
        @Parameter(description = "ID del plan") @PathVariable Integer id) {
        log.info("DELETE /api/planes-viaje/{}", id);
        try {
            planViajeService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error al eliminar plan: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtener segmentos de un plan
     */
    @GetMapping("/{id}/segmentos")
    @Operation(summary = "Segmentos del plan", description = "Obtiene todos los segmentos de vuelo de un plan")
    @ApiResponse(responseCode = "200", description = "Lista de segmentos ordenados")
    public ResponseEntity<List<SegmentoVueloDTO>> obtenerSegmentos(
        @Parameter(description = "ID del plan") @PathVariable Integer id) {
        log.info("GET /api/planes-viaje/{}/segmentos", id);
        List<SegmentoVueloDTO> segmentos = planViajeService.obtenerSegmentosPorPlan(id);
        return ResponseEntity.ok(segmentos);
    }
    
    /**
     * Obtener estadísticas de planes
     */
    @GetMapping("/estadisticas")
    @Operation(summary = "Estadísticas", description = "Obtiene estadísticas generales de todos los planes")
    @ApiResponse(responseCode = "200", description = "Estadísticas de planes")
    public ResponseEntity<EstadisticasPlanesDTO> obtenerEstadisticas() {
        log.info("GET /api/planes-viaje/estadisticas");
        EstadisticasPlanesDTO stats = planViajeService.obtenerEstadisticas();
        return ResponseEntity.ok(stats);
    }
}

