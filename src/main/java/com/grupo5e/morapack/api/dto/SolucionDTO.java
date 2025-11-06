package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para Solución del algoritmo ALNS.
 * Representa una ejecución completa del algoritmo con todas sus métricas.
 * Siguiendo patrón Backend: SolutionSchema
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solución completa generada por el algoritmo ALNS")
public class SolucionDTO {
    
    @Schema(description = "ID de la solución")
    private Integer id;
    
    @Schema(description = "Lista de IDs de rutas que componen esta solución")
    private List<Integer> rutasIds;
    
    @Schema(description = "Costo total de la solución", example = "125000.50")
    private Double costoTotal;
    
    @Schema(description = "Tiempo total en horas", example = "48.5")
    private Double tiempoTotal;
    
    @Schema(description = "Número de paquetes no entregados", example = "3")
    private Integer paquetesNoEntregados;
    
    @Schema(description = "Fitness de la solución (mayor = mejor)", example = "0.85")
    private Double fitness;
    
    @Schema(description = "Algoritmo usado", example = "ALNS")
    private String algoritmoUsado;
    
    @Schema(description = "Número de iteraciones ejecutadas", example = "500")
    private Integer iteracionesEjecutadas;
    
    @Schema(description = "Tiempo de ejecución en segundos", example = "120")
    private Long tiempoEjecucionSegundos;
    
    @Schema(description = "Temperatura final del algoritmo", example = "0.01")
    private Double temperaturaFinal;
    
    @Schema(description = "Total de pedidos procesados", example = "150")
    private Integer totalPedidos;
    
    @Schema(description = "Total de rutas generadas", example = "45")
    private Integer totalRutas;
    
    @Schema(description = "Capacidad de almacenes usada", example = "8500")
    private Integer capacidadAlmacenesUsada;
    
    @Schema(description = "Observaciones sobre la solución")
    private String observaciones;
    
    @Schema(description = "Versión del dataset usado", example = "v1.0")
    private String versionDatos;
    
    @Schema(description = "Porcentaje de pedidos entregados", example = "98.0")
    private Double porcentajeEntregados;
    
    @Schema(description = "Costo promedio por ruta", example = "2777.78")
    private Double costoPromedioRuta;
    
    @Schema(description = "Tiempo promedio por ruta en horas", example = "1.08")
    private Double tiempoPromedioRuta;
    
    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;
    
    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;
}

