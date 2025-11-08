package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO para estadísticas de planes de viaje
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estadísticas generales de planes de viaje")
public class EstadisticasPlanesDTO {
    
    @Schema(description = "Total de planes creados", example = "150")
    private Long totalPlanes;
    
    @Schema(description = "Planes pendientes", example = "20")
    private Long planesPendientes;
    
    @Schema(description = "Planes en progreso", example = "45")
    private Long planesEnProgreso;
    
    @Schema(description = "Planes completados", example = "80")
    private Long planesCompletados;
    
    @Schema(description = "Planes cancelados", example = "5")
    private Long planesCancelados;
    
    @Schema(description = "Costo promedio de planes", example = "12500.75")
    private Double costoPromedio;
    
    @Schema(description = "Tiempo promedio de viaje en horas", example = "18.5")
    private Double tiempoPromedioHoras;
    
    @Schema(description = "Promedio de vuelos por plan", example = "2.8")
    private Double promedioVuelosPorPlan;
}

