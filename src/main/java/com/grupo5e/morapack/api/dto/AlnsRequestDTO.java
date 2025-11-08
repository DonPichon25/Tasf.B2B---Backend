package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para solicitud de ejecución del algoritmo ALNS con soporte para ventanas de tiempo")
public class AlnsRequestDTO {

    // ==================== PARÁMETROS DE VENTANA DE TIEMPO ====================
    
    @Schema(description = "Hora de inicio de la ventana de simulación", 
            example = "2025-01-02T00:00:00",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime horaInicioSimulacion;

    @Schema(description = "Hora de fin de la ventana de simulación (opcional, se calcula desde duración si no se provee)", 
            example = "2025-01-09T00:00:00",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime horaFinSimulacion;

    @Schema(description = "Duración de la simulación en días (alternativa a horaFinSimulacion)", 
            example = "7",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Min(value = 1, message = "La duración debe ser al menos 1 día")
    @Max(value = 30, message = "La duración no puede exceder 30 días")
    private Integer duracionSimulacionDias;

    @Schema(description = "Duración de la simulación en horas (para escenarios incrementales, ej: 0.5 = 30 minutos)", 
            example = "0.5",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Min(value = 0, message = "La duración en horas debe ser positiva")
    @Max(value = 720, message = "La duración no puede exceder 720 horas (30 días)")
    private Double duracionSimulacionHoras;

    // ==================== PARÁMETROS DE DATOS ====================
    
    @Schema(description = "Usar base de datos como fuente (true) o archivos (false)", 
            example = "true",
            defaultValue = "true")
    private Boolean usarBaseDatos;

    @Schema(description = "Lista de IDs de pedidos a optimizar (vacío = todos los pedidos en la ventana de tiempo)")
    private List<Integer> pedidosIds;

    // ==================== PARÁMETROS DEL ALGORITMO ALNS ====================
    
    @Min(value = 1, message = "El número de iteraciones debe ser al menos 1")
    @Max(value = 10000, message = "El número de iteraciones no puede exceder 10000")
    @Schema(description = "Número máximo de iteraciones del algoritmo", 
            example = "1000",
            defaultValue = "1000")
    private Integer maxIteraciones;

    @Schema(description = "Tasa de destrucción para el ALNS (0.0 - 1.0)", 
            example = "0.3",
            defaultValue = "0.3")
    @Min(value = 0, message = "La tasa de destrucción debe estar entre 0 y 1")
    @Max(value = 1, message = "La tasa de destrucción debe estar entre 0 y 1")
    private Double tasaDestruccion;

    @Min(value = 1, message = "El tiempo límite debe ser al menos 1 segundo")
    @Schema(description = "Tiempo límite de ejecución en segundos (0 = sin límite)", 
            example = "300",
            defaultValue = "0")
    private Integer tiempoLimiteSegundos;

    // ==================== PARÁMETROS DE OPTIMIZACIÓN ====================
    
    @Schema(description = "Habilitar modo de unitización de productos", 
            example = "true",
            defaultValue = "true")
    private Boolean habilitarUnitizacion;

    @Schema(description = "Días de horizonte para planificación", 
            example = "4",
            defaultValue = "4")
    @Min(value = 1, message = "El horizonte debe ser al menos 1 día")
    @Max(value = 30, message = "El horizonte no puede exceder 30 días")
    private Integer diasHorizonte;

    // ==================== PARÁMETROS DE DEBUG ====================
    
    @Schema(description = "Modo de logging verboso para debug", 
            example = "false",
            defaultValue = "false")
    private Boolean modoDebug;

    // ==================== CAMPOS DEPRECADOS (mantener compatibilidad) ====================
    
    @Deprecated
    @Schema(description = "DEPRECADO: Use 'usarBaseDatos' en su lugar", 
            deprecated = true,
            allowableValues = {"ARCHIVOS", "BASE_DE_DATOS"})
    private String fuente;
}

