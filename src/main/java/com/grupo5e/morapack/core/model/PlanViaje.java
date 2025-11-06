package com.grupo5e.morapack.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan de viaje asignado a un pedido específico.
 * Representa la ruta completa con segmentos de vuelo calculada por el algoritmo ALNS.
 * Siguiendo patrón de MoraPack-Backend: TravelPlan
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "plan_viaje")
public class PlanViaje {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "fecha_planificacion", nullable = false)
    private LocalDateTime fechaPlanificacion;
    
    @Column(name = "estado", nullable = false, length = 50)
    private String estado; // "PENDIENTE", "EN_PROGRESO", "COMPLETADO", "CANCELADO"
    
    @Column(name = "algoritmo_usado", nullable = false, length = 50)
    private String algoritmoUsado; // "ALNS"
    
    @Column(name = "version_datos", length = 50)
    private String versionDatos; // Para tracking de qué dataset se usó
    
    @Column(name = "costo_total")
    private Double costoTotal;
    
    @Column(name = "tiempo_total_horas")
    private Double tiempoTotalHoras;
    
    @Column(name = "numero_vuelos")
    private Integer numeroVuelos;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relación: Un plan de viaje pertenece a UN pedido
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;
    
    // Relación: Un plan tiene MUCHOS segmentos de vuelo
    @OneToMany(mappedBy = "planViaje", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SegmentoVuelo> segmentosVuelo = new ArrayList<>();
    
    /**
     * Método de conveniencia para agregar segmento
     */
    public void agregarSegmento(SegmentoVuelo segmento) {
        segmentosVuelo.add(segmento);
        segmento.setPlanViaje(this);
    }
    
    /**
     * Método de conveniencia para remover segmento
     */
    public void removerSegmento(SegmentoVuelo segmento) {
        segmentosVuelo.remove(segmento);
        segmento.setPlanViaje(null);
    }
}

