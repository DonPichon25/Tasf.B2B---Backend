package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO con información de un pedido que no pudo ser asignado.
 * Usado para detectar el punto de colapso (primer pedido no asignado cronológicamente).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Información de un pedido no asignado")
public class PedidoNoAsignadoInfoDTO {

    @Schema(description = "ID del pedido", example = "234")
    private Integer id;

    @Schema(description = "Fecha/hora en que se realizó el pedido", example = "2025-01-05T14:30:00")
    private LocalDateTime fechaPedido;

    @Schema(description = "Fecha límite de entrega del pedido", example = "2025-01-07T14:30:00")
    private LocalDateTime fechaLimiteEntrega;

    @Schema(description = "Código IATA del aeropuerto origen", example = "SPIM")
    private String codigoOrigen;

    @Schema(description = "Código IATA del aeropuerto destino", example = "SKBO")
    private String codigoDestino;

    @Schema(description = "Cantidad de productos en el pedido", example = "5")
    private Integer cantidadProductos;

    @Schema(description = "Motivo por el cual no se pudo asignar", example = "No se encontró ruta dentro del deadline")
    private String motivo;
}

