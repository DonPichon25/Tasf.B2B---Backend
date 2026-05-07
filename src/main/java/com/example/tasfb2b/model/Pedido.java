package com.example.tasfb2b.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pedido {
    private String idPedido;          // Ej. 00000001
    private String origen;            // Obtenido del nombre del archivo _envios_SKBO_.txt
    private String destino;           // Ej. EBCI
    private LocalDateTime fechaRegistro; // Mezclando aaaammdd y hh:mm
    private int cantidadMaletas;      // Ej. 6 (Nuestro bloque indivisible)
    private String idCliente;         // Ej. 0007729
}