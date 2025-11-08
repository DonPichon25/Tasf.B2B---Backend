package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.model.Vuelo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta para consultas de vuelos
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de consulta de vuelos con estadísticas y capacidad")
public class RespuestaConsultaVuelosDTO {

    @Schema(description = "Indica si la consulta fue exitosa", example = "true")
    private Boolean exito;

    @Schema(description = "Mensaje descriptivo")
    private String mensaje;

    @Schema(description = "Código del vuelo (si se consulta uno específico)", example = "LIMA-BRUS-001")
    private String codigoVuelo;

    @Schema(description = "Total de vuelos encontrados", example = "150")
    private Integer totalVuelos;

    @Schema(description = "Lista de vuelos")
    private List<Vuelo> vuelos;

    @Schema(description = "Vuelo específico consultado")
    private Vuelo vuelo;

    @Schema(description = "Desglose de vuelos por estado")
    private Map<String, Long> desglosePorEstado;

    @Schema(description = "Capacidad usada del vuelo", example = "250")
    private Integer capacidadUsada;

    @Schema(description = "Capacidad total del vuelo", example = "300")
    private Integer capacidadTotal;

    @Schema(description = "Capacidad disponible del vuelo", example = "50")
    private Integer capacidadDisponible;

    @Schema(description = "Cantidad de productos en el vuelo", example = "250")
    private Integer cantidadProductos;

    @Schema(description = "Cantidad de pedidos en el vuelo", example = "45")
    private Integer cantidadPedidos;
}

