package com.grupo5e.morapack.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Segmento individual de vuelo dentro de un plan de viaje.
 * Representa un vuelo específico en la ruta de un pedido.
 * Siguiendo patrón de MoraPack-Backend: FlightSegment
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "segmento_vuelo")
public class SegmentoVuelo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "orden_segmento", nullable = false)
    private Integer ordenSegmento; // Orden en la secuencia (1, 2, 3, etc.)
    
    // ETD = Estimated Time of Departure (Hora estimada de salida)
    @Column(name = "hora_salida_estimada", nullable = false)
    private LocalDateTime horaSalidaEstimada;
    
    // ETA = Estimated Time of Arrival (Hora estimada de llegada)
    @Column(name = "hora_llegada_estimada", nullable = false)
    private LocalDateTime horaLlegadaEstimada;
    
    @Column(name = "capacidad_reservada", nullable = false)
    private Integer capacidadReservada;
    
    @Column(name = "codigo_origen", length = 10)
    private String codigoOrigen; // IATA del aeropuerto origen
    
    @Column(name = "codigo_destino", length = 10)
    private String codigoDestino; // IATA del aeropuerto destino
    
    @Column(name = "duracion_horas")
    private Double duracionHoras;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Relación: Pertenece a un PlanViaje
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_viaje_id", nullable = false)
    private PlanViaje planViaje;
    
    // Relación: Opcional - asociado al vuelo específico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vuelo_id")
    private Vuelo vuelo;
    
    // Relación: Asociado al pedido (para queries directas)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;
}

