package com.grupo5e.morapack.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para Ruta.
 * Representa una ruta individual dentro de una solución.
 * Siguiendo patrón Backend: RouteSchema
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ruta individual de vuelos desde origen hasta destino")
public class RutaDTO {
    
    @Schema(description = "ID de la ruta")
    private Integer id;
    
    @Schema(description = "Lista de IDs de vuelos que componen esta ruta")
    private List<Integer> vuelosIds;
    
    @Schema(description = "ID del aeropuerto origen", example = "1")
    private Integer aeropuertoOrigenId;
    
    @Schema(description = "Código IATA del aeropuerto origen", example = "SKBO")
    private String codigoOrigenIATA;
    
    @Schema(description = "Nombre del aeropuerto origen", example = "El Dorado")
    private String nombreOrigen;
    
    @Schema(description = "ID del aeropuerto destino", example = "5")
    private Integer aeropuertoDestinoId;
    
    @Schema(description = "Código IATA del aeropuerto destino", example = "EDDI")
    private String codigoDestinoIATA;
    
    @Schema(description = "Nombre del aeropuerto destino", example = "Tempelhof")
    private String nombreDestino;
    
    @Schema(description = "Tiempo total de la ruta en horas", example = "12.5")
    private Double tiempoTotal;
    
    @Schema(description = "Costo total de la ruta", example = "3500.00")
    private Double costoTotal;
    
    @Schema(description = "Lista de IDs de pedidos asignados a esta ruta")
    private List<Integer> pedidosIds;
    
    @Schema(description = "ID de la solución a la que pertenece esta ruta")
    private Integer solucionId;
    
    @Schema(description = "Número de vuelos en la ruta", example = "3")
    private Integer numeroVuelos;
    
    @Schema(description = "Distancia total en kilómetros", example = "8500.5")
    private Double distanciaTotalKm;
    
    @Schema(description = "Capacidad utilizada", example = "450")
    private Integer capacidadUtilizada;
    
    @Schema(description = "Orden de la ruta en la solución", example = "1")
    private Integer ordenRuta;
    
    @Schema(description = "Costo promedio por vuelo", example = "1166.67")
    private Double costoPromedioVuelo;
    
    @Schema(description = "Tiempo promedio por vuelo en horas", example = "4.17")
    private Double tiempoPromedioVuelo;
    
    @Schema(description = "Fecha de creación")
    private LocalDateTime createdAt;
    
    @Schema(description = "Fecha de última actualización")
    private LocalDateTime updatedAt;
}
