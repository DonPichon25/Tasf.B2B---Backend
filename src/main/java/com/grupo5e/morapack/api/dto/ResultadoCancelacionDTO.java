package com.grupo5e.morapack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para el resultado de una operación de cancelación de vuelo.
 * 
 * Incluye información sobre:
 * - Si la cancelación fue exitosa
 * - Productos afectados por la cancelación
 * - Productos que pudieron ser re-asignados automáticamente
 * - Productos que quedaron sin asignar y requieren re-optimización
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoCancelacionDTO {

    /**
     * Indica si la cancelación fue exitosa.
     */
    private Boolean exitoso;

    /**
     * ID del vuelo cancelado.
     */
    private Integer vueloId;
    // Info de la instancia cancelada
    private String idInstancia;
    private Integer vueloBaseId;
    private String origen;
    private String destino;
    private Integer pedidosAfectados;
    /**
     * Mensaje descriptivo del resultado de la operación.
     */
    private String mensaje;
    
    /**
     * Número total de productos afectados por la cancelación.
     */
    private Integer productosAfectados;
    
    /**
     * Número de productos que fueron re-asignados automáticamente a rutas alternativas.
     */
    private Integer productosReasignados;
    
    /**
     * Número de productos que no pudieron ser re-asignados automáticamente.
     * Estos productos requieren una re-optimización completa del plan.
     */
    private Integer productosSinAsignar;
    
    /**
     * Indica si se requiere re-ejecutar el algoritmo para los productos sin asignar.
     */
    @Builder.Default
    private Boolean requiereReoptimizacion = false;
    
    /**
     * Tiempo estimado de la re-optimización en segundos (si aplica).
     */
    private Integer tiempoEstimadoReoptimizacion;
}

