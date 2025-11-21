package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.ResultadoAsignacionIncrementalDTO;
import com.grupo5e.morapack.api.dto.SolicitudAsignacionIncrementalDTO;
import com.grupo5e.morapack.service.ServicioAsignacionIncremental;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para asignación incremental de pedidos.
 * 
 * Permite agregar nuevos pedidos durante una simulación activa sin re-ejecutar
 * el algoritmo completo. Busca espacio disponible en las capacidades actuales
 * de vuelos y almacenes.
 * 
 * ENDPOINTS:
 * - POST /api/asignacion-incremental: Asignar pedido sin re-optimizar plan completo
 */
@RestController
@RequestMapping("/api/asignacion-incremental")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Asignación Incremental", description = "API para asignación de pedidos sin re-optimización completa")
public class AsignacionIncrementalController {
    
    private final ServicioAsignacionIncremental servicioAsignacion;
    
    /**
     * Asigna un pedido nuevo sin re-optimizar el plan completo.
     * 
     * FLUJO:
     * 1. Valida que el pedido exista
     * 2. Carga capacidades actuales de vuelos (productos ya asignados)
     * 3. Busca espacio disponible en vuelos existentes
     * 4. Si hay espacio, asigna el pedido y actualiza capacidades
     * 5. Si no hay espacio, retorna error indicando que se requiere re-optimización
     * 
     * VENTAJAS:
     * - Rápido: no re-ejecuta el algoritmo completo
     * - No interrumpe simulación activa
     * - Mantiene consistencia de capacidades
     * 
     * LIMITACIONES:
     * - Solo funciona si hay capacidad disponible
     * - No re-optimiza rutas existentes para hacer espacio
     * - Si falla, requiere ejecutar algoritmo completo
     * 
     * @param solicitud Solicitud de asignación incremental
     * @return Resultado de la asignación con estadísticas
     */
    @Operation(
        summary = "Asignar pedido sin re-optimizar plan completo",
        description = "Intenta asignar un nuevo pedido usando solo el espacio disponible en vuelos actuales, " +
                     "sin re-ejecutar el algoritmo completo."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Pedido asignado exitosamente",
            content = @Content(schema = @Schema(implementation = ResultadoAsignacionIncrementalDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Solicitud inválida o pedido no encontrado"
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "No hay capacidad disponible - se requiere re-optimización"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Error interno al procesar asignación"
        )
    })
    @PostMapping
    public ResponseEntity<ResultadoAsignacionIncrementalDTO> asignarPedido(
            @Parameter(description = "Datos de la solicitud de asignación incremental", required = true)
            @Valid @RequestBody SolicitudAsignacionIncrementalDTO solicitud) {
        
        log.info("========================================");
        log.info("SOLICITUD DE ASIGNACIÓN INCREMENTAL");
        log.info("Pedido ID: {}", solicitud.getPedidoId());
        log.info("Tiempo actual: {}", solicitud.getTiempoSimulacionActual());
        log.info("Forzar re-optimización si no hay espacio: {}", 
                 solicitud.getForzarReoptimizacionSiNoHayEspacio());
        log.info("========================================");
        
        try {
            // Ejecutar asignación incremental
            ResultadoAsignacionIncrementalDTO resultado = 
                servicioAsignacion.asignarPedidoIncremental(
                    solicitud.getPedidoId(),
                    solicitud.getTiempoSimulacionActual()
                );
            
            // Determinar código de respuesta según resultado
            if (resultado.getExitoso()) {
                log.info("✅ Asignación incremental exitosa para pedido {}", solicitud.getPedidoId());
                return ResponseEntity.ok(resultado);
            } else {
                log.warn("✗ Asignación incremental fallida para pedido {}: {}", 
                         solicitud.getPedidoId(), resultado.getMensaje());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(resultado);
            }
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación en asignación incremental: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResultadoAsignacionIncrementalDTO.builder()
                    .exitoso(false)
                    .pedidoId(solicitud.getPedidoId())
                    .mensaje("Error de validación: " + e.getMessage())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error inesperado en asignación incremental", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResultadoAsignacionIncrementalDTO.builder()
                    .exitoso(false)
                    .pedidoId(solicitud.getPedidoId())
                    .mensaje("Error interno: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * Endpoint simplificado que solo requiere el ID del pedido.
     * Usa el tiempo actual del sistema como tiempo de simulación.
     * 
     * @param pedidoId ID del pedido a asignar
     * @return Resultado de la asignación
     */
    @Operation(
        summary = "Asignar pedido (versión simplificada)",
        description = "Asigna un pedido usando solo su ID. Usa el tiempo actual del sistema."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pedido asignado exitosamente"),
        @ApiResponse(responseCode = "409", description = "No hay capacidad disponible")
    })
    @PostMapping("/simple/{pedidoId}")
    public ResponseEntity<ResultadoAsignacionIncrementalDTO> asignarPedidoSimple(
            @Parameter(description = "ID del pedido", required = true)
            @PathVariable Integer pedidoId) {
        
        // Crear solicitud con valores por defecto
        SolicitudAsignacionIncrementalDTO solicitud = SolicitudAsignacionIncrementalDTO.builder()
            .pedidoId(pedidoId)
            .tiempoSimulacionActual(java.time.LocalDateTime.now())
            .forzarReoptimizacionSiNoHayEspacio(false)
            .ventanaMaximaHoras(24)
            .build();
        
        return asignarPedido(solicitud);
    }
}

