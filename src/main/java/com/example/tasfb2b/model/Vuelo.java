package com.example.tasfb2b.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Vuelo {
    private String origen;         // Ej. SKBO
    private String destino;        // Ej. SEQM
    private LocalTime horaSalida;  // Ej. 03:34
    private LocalTime horaLlegada; // Ej. 04:21
    private int capacidadMax;      // Ej. 300
}