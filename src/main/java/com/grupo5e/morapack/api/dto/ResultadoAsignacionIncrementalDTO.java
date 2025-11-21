package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.model.Producto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para el resultado de una asignación incremental de pedido.
 * 
 * Indica si el pedido pudo ser asignado sin re-optimizar el plan completo,
 * o si se requiere una re-optimización debido a falta de capacidad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoAsignacionIncrementalDTO {
    
    /**
     * Indica si la asignación incremental fue exitosa.
     */
    private Boolean exitoso;
    
    /**
     * ID del pedido procesado.
     */
    private Integer pedidoId;
    
    /**
     * Mensaje descriptivo del resultado.
     */
    private String mensaje;
    
    /**
     * Número de productos asignados exitosamente.
     */
    private Integer productosAsignados;
    
    /**
     * Código del vuelo al que se asignó el pedido (si aplica).
     * Para rutas multi-tramo, este es el código del primer vuelo.
     */
    private String codigoVuelo;
    
    /**
     * Lista de vuelos en la ruta asignada (códigos IATA).
     * Ejemplo: ["SPIM-SCEL", "SCEL-SKBO"]
     */
    private List<String> rutaAsignada;
    
    /**
     * Lista completa de productos creados y asignados.
     * Incluye IDs y estado de cada producto.
     */
    private List<Producto> productosCreados;
    
    /**
     * Indica si se ejecutó una re-optimización completa porque no había espacio.
     */
    @Builder.Default
    private Boolean seEjecutoReoptimizacion = false;
    
    /**
     * Tiempo de ejecución de la operación en milisegundos.
     */
    private Long tiempoEjecucionMs;
    
    /**
     * Capacidad disponible restante en el vuelo asignado.
     */
    private Integer capacidadDisponibleRestante;
}

