package com.grupo5e.morapack.api.dto;

import com.grupo5e.morapack.core.enums.EstadoVuelo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "DTO para representar un vuelo sin relaciones anidadas")
public class VueloDTO {
    
    @Schema(description = "ID único del vuelo", example = "1")
    private Integer id;
    
    @Schema(description = "Código identificador del vuelo", example = "SPIM-SKBO-08:30")
    private String codigo;
    
    @Schema(description = "Frecuencia de vuelos por día", example = "2.0")
    private Double frecuenciaPorDia;
    
    @Schema(description = "Hora de salida", example = "08:30:00")
    private LocalTime horaSalida;
    
    @Schema(description = "Hora de llegada", example = "11:45:00")
    private LocalTime horaLlegada;
    
    // IDs en lugar de objetos completos (evita lazy loading)
    @Schema(description = "ID del aeropuerto de origen", example = "1")
    private Integer aeropuertoOrigenId;
    
    @Schema(description = "Código IATA del aeropuerto de origen", example = "SPIM")
    private String aeropuertoOrigenCodigo;
    
    @Schema(description = "ID del aeropuerto de destino", example = "2")
    private Integer aeropuertoDestinoId;
    
    @Schema(description = "Código IATA del aeropuerto de destino", example = "SKBO")
    private String aeropuertoDestinoCodigo;
    
    @Schema(description = "Capacidad máxima del vuelo", example = "100")
    private Integer capacidadMaxima;
    
    @Schema(description = "Capacidad actualmente utilizada", example = "45")
    private Integer capacidadUsada;
    
    @Schema(description = "Tiempo de transporte en horas", example = "3.25")
    private Double tiempoTransporte;
    
    @Schema(description = "Costo del vuelo", example = "450.50")
    private Double costo;
    
    @Schema(description = "Estado actual del vuelo", example = "DISPONIBLE")
    private EstadoVuelo estado;
    
    // Coordenadas actuales (opcional, para tracking en tiempo real)
    @Schema(description = "Latitud actual del vuelo")
    private String latitudActual;
    
    @Schema(description = "Longitud actual del vuelo")
    private String longitudActual;
}
