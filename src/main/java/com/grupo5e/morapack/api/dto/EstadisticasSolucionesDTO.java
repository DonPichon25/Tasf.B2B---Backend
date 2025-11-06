package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO para estadísticas agregadas de soluciones.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estadísticas agregadas de todas las soluciones")
public class EstadisticasSolucionesDTO {
    
    @Schema(description = "Total de soluciones generadas", example = "250")
    private Long totalSoluciones;
    
    @Schema(description = "Fitness promedio de todas las soluciones", example = "0.78")
    private Double fitnessPromedio;
    
    @Schema(description = "Mejor fitness obtenido", example = "0.95")
    private Double mejorFitness;
    
    @Schema(description = "Peor fitness obtenido", example = "0.45")
    private Double peorFitness;
    
    @Schema(description = "Costo promedio de soluciones", example = "98500.75")
    private Double costoPromedio;
    
    @Schema(description = "Costo más bajo obtenido", example = "65000.00")
    private Double costoMinimo;
    
    @Schema(description = "Costo más alto obtenido", example = "145000.00")
    private Double costoMaximo;
    
    @Schema(description = "Tiempo promedio en horas", example = "36.5")
    private Double tiempoPromedio;
    
    @Schema(description = "Tiempo mínimo en horas", example = "24.0")
    private Double tiempoMinimo;
    
    @Schema(description = "Tiempo máximo en horas", example = "56.0")
    private Double tiempoMaximo;
    
    @Schema(description = "Promedio de paquetes no entregados", example = "2.5")
    private Double promedioNoEntregados;
    
    @Schema(description = "Número de soluciones perfectas (0 no entregados)", example = "85")
    private Long solucionesPerfectas;
    
    @Schema(description = "Porcentaje de soluciones perfectas", example = "34.0")
    private Double porcentajeSolucionesPerfectas;
    
    @Schema(description = "Promedio de rutas por solución", example = "42.8")
    private Double promedioRutasPorSolucion;
    
    @Schema(description = "Promedio de tiempo de ejecución en segundos", example = "95.5")
    private Double promedioTiempoEjecucion;
    
    @Schema(description = "Promedio de iteraciones ejecutadas", example = "485")
    private Double promedioIteraciones;
    
    @Schema(description = "Total de pedidos procesados (suma de todas las soluciones)", example = "37500")
    private Long totalPedidosProcesados;
    
    @Schema(description = "Total de rutas generadas (suma de todas las soluciones)", example = "10700")
    private Long totalRutasGeneradas;
}

