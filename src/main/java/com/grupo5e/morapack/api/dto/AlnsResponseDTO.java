package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO de respuesta con los resultados del algoritmo ALNS con métricas detalladas y ventanas de tiempo")
public class AlnsResponseDTO {

    // ==================== ESTADO DE LA EJECUCIÓN ====================
    
    @Schema(description = "Indica si la ejecución fue exitosa", example = "true")
    private Boolean exito;

    @Schema(description = "Mensaje descriptivo del resultado")
    private String mensaje;

    @Schema(description = "ID único de la ejecución (si se persistió)", example = "1")
    private Long ejecucionId;

    // ==================== MÉTRICAS DE TIEMPO DE EJECUCIÓN ====================
    
    @Schema(description = "Fecha y hora de inicio de la ejecución del algoritmo", example = "2025-01-15T10:00:00")
    private LocalDateTime tiempoInicioEjecucion;

    @Schema(description = "Fecha y hora de finalización de la ejecución del algoritmo", example = "2025-01-15T10:05:00")
    private LocalDateTime tiempoFinEjecucion;

    @Schema(description = "Duración de la ejecución en segundos", example = "300")
    private Long tiempoEjecucionSegundos;

    // ==================== VENTANA DE SIMULACIÓN ====================
    
    @Schema(description = "Hora de inicio de la ventana de simulación procesada", example = "2025-01-02T00:00:00")
    private LocalDateTime horaInicioSimulacion;

    @Schema(description = "Hora de fin de la ventana de simulación procesada", example = "2025-01-09T00:00:00")
    private LocalDateTime horaFinSimulacion;

    // ==================== MÉTRICAS A NIVEL DE PEDIDOS ====================
    
    @Schema(description = "Número total de pedidos procesados", example = "150")
    private Integer totalPedidos;

    @Schema(description = "Número de pedidos asignados exitosamente", example = "145")
    private Integer pedidosAsignados;

    @Schema(description = "Número de pedidos no asignados", example = "5")
    private Integer pedidosNoAsignados;

    @Schema(description = "Lista de IDs de pedidos no asignados")
    private List<Integer> pedidosNoAsignadosIds;

    // ==================== MÉTRICAS A NIVEL DE PRODUCTOS (NUEVO) ====================
    
    @Schema(description = "Número total de productos procesados", example = "3750")
    private Integer totalProductos;

    @Schema(description = "Número de productos asignados a rutas", example = "3625")
    private Integer productosAsignados;

    @Schema(description = "Número de productos no asignados", example = "125")
    private Integer productosNoAsignados;

    @Schema(description = "Número de productos persistidos en base de datos", example = "3625")
    private Integer productosPersistidos;

    // ==================== MÉTRICAS DE CALIDAD DE LA SOLUCIÓN ====================
    
    @Schema(description = "Costo total de la solución", example = "125000.50")
    private Double costoTotal;

    @Schema(description = "Puntaje de calidad de la solución", example = "0.95")
    private Double puntaje;

    @Schema(description = "Tiempo total de entrega promedio en horas", example = "24.5")
    private Double tiempoPromedioEntrega;

    @Schema(description = "Indica si la solución es válida", example = "true")
    private Boolean solucionValida;

    @Schema(description = "Indica si se respetan las capacidades", example = "true")
    private Boolean capacidadValida;

    // ==================== DATOS DE LA SOLUCIÓN ====================
    
    @Schema(description = "Rutas de productos: información detallada de asignaciones")
    private List<RutaProductoDTO> rutasProductos;

    @Schema(description = "Línea de tiempo de simulación con eventos de vuelos")
    private LineaDeTiempoSimulacionDTO lineaDeTiempo;

    @Schema(description = "Solución cruda para debugging (opcional)")
    private Map<String, Object> solucionCruda;

    // ==================== INFORMACIÓN DEL ALGORITMO ====================
    
    @Schema(description = "Número de iteraciones ejecutadas", example = "1000")
    private Integer iteracionesEjecutadas;

    @Schema(description = "Mejor costo encontrado durante la ejecución", example = "120000.00")
    private Double mejorCosto;

    @Schema(description = "Detalles adicionales o advertencias")
    private List<String> advertencias;

    // ==================== CAMPOS DEPRECADOS (mantener compatibilidad) ====================
    
    @Deprecated
    @Schema(description = "DEPRECADO: Use 'tiempoInicioEjecucion' en su lugar", deprecated = true)
    private LocalDateTime fechaInicio;

    @Deprecated
    @Schema(description = "DEPRECADO: Use 'tiempoFinEjecucion' en su lugar", deprecated = true)
    private LocalDateTime fechaFin;

    @Deprecated
    @Schema(description = "DEPRECADO: Use 'tiempoEjecucionSegundos' en su lugar", deprecated = true)
    private Long duracionMs;

    @Deprecated
    @Schema(description = "DEPRECADO: Use 'exito' en su lugar", deprecated = true)
    private String estado;

    @Deprecated
    @Schema(description = "DEPRECADO: Use 'rutasProductos' en su lugar", deprecated = true)
    private Map<Long, List<Integer>> solucion;
}

