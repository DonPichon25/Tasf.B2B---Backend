package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.model.Pedido;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta para consultas de pedidos
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Respuesta de consulta de pedidos con estadísticas y ventana de tiempo")
public class RespuestaConsultaPedidosDTO {

    @Schema(description = "Indica si la consulta fue exitosa", example = "true")
    private Boolean exito;

    @Schema(description = "Mensaje descriptivo")
    private String mensaje;

    @Schema(description = "Total de pedidos encontrados", example = "150")
    private Integer totalPedidos;

    @Schema(description = "Lista de pedidos")
    private List<Pedido> pedidos;

    @Schema(description = "Ventana de tiempo utilizada para filtrar")
    private Map<String, String> ventanaTiempo;
}

