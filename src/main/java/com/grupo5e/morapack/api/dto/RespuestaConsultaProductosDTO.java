package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.model.Producto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta para consultas de productos
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de consulta de productos con estadísticas por estado")
public class RespuestaConsultaProductosDTO {

    @Schema(description = "Indica si la consulta fue exitosa", example = "true")
    private Boolean exito;

    @Schema(description = "Mensaje descriptivo")
    private String mensaje;

    @Schema(description = "ID del pedido consultado (si aplica)", example = "12345")
    private Integer idPedido;

    @Schema(description = "Total de productos encontrados", example = "3750")
    private Integer totalProductos;

    @Schema(description = "Cantidad de productos encontrados", example = "45")
    private Integer cantidadProductos;

    @Schema(description = "Lista de productos")
    private List<Producto> productos;

    @Schema(description = "Desglose de productos por estado")
    private Map<String, Long> desglosePorEstado;
}

