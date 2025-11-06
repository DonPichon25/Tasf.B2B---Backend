package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para respuesta del algoritmo ALNS con tracking a nivel de producto.
 * Siguiendo patrón de MoraPack-Backend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resultado de la ejecución del algoritmo ALNS con rutas a nivel de producto")
public class ResultadoAlgoritmoDTO {
    
    @Schema(description = "Indica si el algoritmo ejecutó exitosamente", example = "true")
    private Boolean exitoso;
    
    @Schema(description = "Mensaje descriptivo del resultado", example = "Algoritmo ejecutado exitosamente")
    private String mensaje;
    
    @Schema(description = "Hora de inicio de la ejecución", example = "2024-01-15T10:00:00")
    private LocalDateTime horaInicio;
    
    @Schema(description = "Hora de finalización de la ejecución", example = "2024-01-15T10:05:30")
    private LocalDateTime horaFin;
    
    @Schema(description = "Duración de la ejecución en segundos", example = "330")
    private Long segundosEjecucion;
    
    @Schema(description = "Total de productos procesados", example = "450")
    private Integer totalProductos;
    
    @Schema(description = "Total de pedidos procesados", example = "150")
    private Integer totalPedidos;
    
    @Schema(description = "Rutas asignadas a cada producto")
    private List<RutaProductoDTO> rutasProductos;
    
    @Schema(description = "Costo total de la solución", example = "125000.50")
    private Double costoTotal;
    
    @Schema(description = "Porcentaje de productos asignados", example = "96.5")
    private Double porcentajeAsignacion;
    
    @Schema(description = "Timeline temporal de la simulación con eventos ordenados")
    private LineaDeTiempoSimulacionDTO lineaDeTiempo;
}

