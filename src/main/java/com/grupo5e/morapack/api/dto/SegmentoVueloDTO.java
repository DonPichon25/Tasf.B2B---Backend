package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO para Segmento de Vuelo.
 * Representa un vuelo individual en la ruta de un pedido.
 * Siguiendo patrón Backend: FlightSegmentDTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Segmento individual de vuelo en un plan de viaje")
public class SegmentoVueloDTO {
    
    @Schema(description = "ID del segmento")
    private Integer id;
    
    @Schema(description = "Orden del segmento en la ruta", example = "1")
    private Integer ordenSegmento;
    
    @Schema(description = "Hora estimada de salida (ETD)", example = "2024-01-15T10:00:00")
    private LocalDateTime horaSalidaEstimada;
    
    @Schema(description = "Hora estimada de llegada (ETA)", example = "2024-01-15T14:00:00")
    private LocalDateTime horaLlegadaEstimada;
    
    @Schema(description = "Capacidad reservada en el vuelo", example = "150")
    private Integer capacidadReservada;
    
    @Schema(description = "Código IATA del aeropuerto origen", example = "SKBO")
    private String codigoOrigen;
    
    @Schema(description = "Código IATA del aeropuerto destino", example = "EDDI")
    private String codigoDestino;
    
    @Schema(description = "Duración del vuelo en horas", example = "4.0")
    private Double duracionHoras;
    
    @Schema(description = "ID del plan de viaje asociado")
    private Integer planViajeId;
    
    @Schema(description = "ID del vuelo específico (si aplica)")
    private Integer vueloId;
    
    @Schema(description = "ID del pedido")
    private Integer pedidoId;
    
    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;
}

