package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para Plan de Viaje.
 * Representa la ruta completa asignada a un pedido.
 * Siguiendo patrón Backend: TravelPlanSchema
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plan de viaje con segmentos de vuelo para un pedido")
public class PlanViajeDTO {
    
    @Schema(description = "ID del plan de viaje")
    private Integer id;
    
    @Schema(description = "Fecha de planificación", example = "2024-01-15T10:00:00")
    private LocalDateTime fechaPlanificacion;
    
    @Schema(description = "Estado del plan", example = "COMPLETADO", allowableValues = {"PENDIENTE", "EN_PROGRESO", "COMPLETADO", "CANCELADO"})
    private String estado;
    
    @Schema(description = "Algoritmo usado", example = "ALNS")
    private String algoritmoUsado;
    
    @Schema(description = "Versión del dataset usado", example = "v1.0")
    private String versionDatos;
    
    @Schema(description = "Costo total estimado", example = "15000.50")
    private Double costoTotal;
    
    @Schema(description = "Tiempo total en horas", example = "24.5")
    private Double tiempoTotalHoras;
    
    @Schema(description = "Número de vuelos en la ruta", example = "3")
    private Integer numeroVuelos;
    
    @Schema(description = "ID del pedido asociado", example = "123")
    private Integer pedidoId;
    
    @Schema(description = "Segmentos de vuelo ordenados")
    private List<SegmentoVueloDTO> segmentosVuelo;
    
    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;
    
    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;
}

