package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO para estadísticas de asignación de productos a vuelos
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Estadísticas de asignación de productos a vuelos")
public class EstadisticasAsignacionDTO {

    @Schema(description = "Indica si la consulta fue exitosa", example = "true")
    private Boolean exito;

    @Schema(description = "Mensaje descriptivo")
    private String mensaje;

    @Schema(description = "Total de productos en el sistema", example = "8500")
    private Integer totalProductos;

    @Schema(description = "Productos asignados a vuelos", example = "8000")
    private Long productosAsignados;

    @Schema(description = "Productos no asignados", example = "500")
    private Long productosNoAsignados;

    @Schema(description = "Tasa de asignación en porcentaje", example = "94.12")
    private Double tasaAsignacion;
}

