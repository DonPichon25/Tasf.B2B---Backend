package com.grupo5e.morapack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para solicitar la asignación incremental de un nuevo pedido.
 * 
 * La asignación incremental intenta asignar el pedido sin re-optimizar todo el plan,
 * buscando espacio disponible en las capacidades actuales de vuelos y almacenes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudAsignacionIncrementalDTO {
    
    /**
     * ID del pedido a asignar incrementalmente.
     */
    private Integer pedidoId;
    
    /**
     * Tiempo actual de la simulación.
     * Se usa para buscar vuelos disponibles a partir de este momento.
     */
    private LocalDateTime tiempoSimulacionActual;
    
    /**
     * Indica si se debe forzar la re-optimización si no hay espacio disponible.
     * Si es false, la operación falla si no hay capacidad.
     * Si es true, se ejecuta el algoritmo completo si la asignación incremental no es posible.
     */
    @Builder.Default
    private Boolean forzarReoptimizacionSiNoHayEspacio = false;
    
    /**
     * Ventana de tiempo máxima para buscar vuelos disponibles (en horas).
     * Por defecto: 24 horas desde tiempoSimulacionActual.
     */
    @Builder.Default
    private Integer ventanaMaximaHoras = 24;
}

