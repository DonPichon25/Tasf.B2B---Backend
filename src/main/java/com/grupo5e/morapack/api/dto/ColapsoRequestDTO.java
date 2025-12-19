package com.grupo5e.morapack.api.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColapsoRequestDTO {
    private String sessionId; // El ID de la RAM
    private LocalDateTime horaInicioSimulacion;
}