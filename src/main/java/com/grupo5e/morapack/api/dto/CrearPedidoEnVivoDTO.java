package com.grupo5e.morapack.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// CrearPedidoEnVivoDTO.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearPedidoEnVivoDTO {

    private String aeropuertoDestinoCodigo;
    private Integer cantidadProductos;
    private LocalDateTime fechaPedido;   // viene del front = inicio próxima ventana
    private Integer tipoData;           // normalmente 1

    // getters y setters
}
