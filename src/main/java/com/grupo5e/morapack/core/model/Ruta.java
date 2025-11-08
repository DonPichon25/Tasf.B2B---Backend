package com.grupo5e.morapack.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una ruta individual dentro de una solución.
 * Una ruta es una secuencia de vuelos que conecta origen con destino.
 * Siguiendo patrón de MoraPack-Backend: Route
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "rutas")
public class Ruta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    
    /**
     * Relación: Una ruta puede usar múltiples vuelos.
     * ManyToMany porque un vuelo puede estar en múltiples rutas.
     * Tabla intermedia: ruta_vuelos
     */
    @ManyToMany
    @JoinTable(
        name = "ruta_vuelos",
        joinColumns = @JoinColumn(name = "ruta_id"),
        inverseJoinColumns = @JoinColumn(name = "vuelo_id")
    )
    @Builder.Default
    private List<Vuelo> vuelos = new ArrayList<>();
    
    /**
     * Aeropuerto de origen de la ruta.
     * Backend usa Ciudad, pero mantenemos Aeropuerto para mayor precisión.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aeropuerto_origen_id", nullable = false)
    private Aeropuerto aeropuertoOrigen;
    
    /**
     * Aeropuerto de destino de la ruta.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aeropuerto_destino_id", nullable = false)
    private Aeropuerto aeropuertoDestino;
    
    /**
     * Tiempo total de la ruta en horas (suma de tiempos de vuelos + esperas).
     */
    @Column(name = "tiempo_total", nullable = false)
    private Double tiempoTotal;
    
    /**
     * Costo total de la ruta (suma de costos de todos los vuelos).
     */
    @Column(name = "costo_total", nullable = false)
    private Double costoTotal;
    
    /**
     * Relación: Una ruta puede transportar múltiples pedidos.
     * ManyToMany porque un pedido puede necesitar múltiples rutas si se divide.
     * Tabla intermedia: ruta_pedidos
     */
    @ManyToMany
    @JoinTable(
        name = "ruta_pedidos",
        joinColumns = @JoinColumn(name = "ruta_id"),
        inverseJoinColumns = @JoinColumn(name = "pedido_id")
    )
    @Builder.Default
    private List<Pedido> pedidos = new ArrayList<>();
    
    /**
     * Relación: Una ruta pertenece a UNA solución.
     * Una solución contiene múltiples rutas.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solucion_id")
    private Solucion solucion;
    
    /**
     * Número de segmentos/vuelos en esta ruta.
     */
    @Column(name = "numero_vuelos")
    private Integer numeroVuelos;
    
    /**
     * Distancia total de la ruta en km (opcional).
     */
    @Column(name = "distancia_total_km")
    private Double distanciaTotalKm;
    
    /**
     * Capacidad total utilizada en esta ruta.
     */
    @Column(name = "capacidad_utilizada")
    private Integer capacidadUtilizada;
    
    /**
     * Orden de la ruta dentro de la solución (para ordenamiento).
     */
    @Column(name = "orden_ruta")
    private Integer ordenRuta;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Método de conveniencia para agregar un vuelo a la ruta.
     */
    public void agregarVuelo(Vuelo vuelo) {
        if (vuelos == null) {
            vuelos = new ArrayList<>();
        }
        vuelos.add(vuelo);
    }
    
    /**
     * Método de conveniencia para agregar un pedido a la ruta.
     */
    public void agregarPedido(Pedido pedido) {
        if (pedidos == null) {
            pedidos = new ArrayList<>();
        }
        pedidos.add(pedido);
    }
    
    /**
     * Calcula el costo promedio por vuelo.
     */
    public double calcularCostoPromedioVuelo() {
        if (numeroVuelos == null || numeroVuelos == 0 || costoTotal == null) {
            return 0.0;
        }
        return costoTotal / numeroVuelos;
    }
    
    /**
     * Calcula el tiempo promedio por vuelo en horas.
     */
    public double calcularTiempoPromedioVuelo() {
        if (numeroVuelos == null || numeroVuelos == 0 || tiempoTotal == null) {
            return 0.0;
        }
        return tiempoTotal / numeroVuelos;
    }
}
