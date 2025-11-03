package com.grupo5e.morapack.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Entidad para almacenar las asignaciones de la simulación (solución del ALNS)
 * Cada registro representa un vuelo asignado a un pedido
 * 
 * Soporta dos modos:
 * 1. Modo BD: vuelo_id y pedido_id apuntan a entidades en BD
 * 2. Modo Temporal: vuelo_id y pedido_id son NULL, datos guardados desnormalizados
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "simulacion_asignacion")
public class SimulacionAsignacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "simulacion_id", nullable = false)
    private SimulacionSemanal simulacion;

    // ==================== FKs OPCIONALES (nullable para modo temporal) ====================
    
    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = true)
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "vuelo_id", nullable = true)
    private Vuelo vuelo;

    // ==================== CAMPOS COMUNES ====================
    
    @Column(name = "secuencia")
    private Integer secuencia; // Orden en la ruta (1, 2, 3...)

    @Column(name = "minuto_inicio")
    private Integer minutoInicio; // Minuto desde T0 donde inicia este vuelo para este paquete

    @Column(name = "minuto_fin")
    private Integer minutoFin; // Minuto donde el paquete llega al destino de este vuelo

    @Column(name = "latitud_inicio")
    private Double latitudInicio;

    @Column(name = "longitud_inicio")
    private Double longitudInicio;

    @Column(name = "latitud_fin")
    private Double latitudFin;

    @Column(name = "longitud_fin")
    private Double longitudFin;
    
    // ==================== FLAG DE MODO ====================
    
    @Column(name = "es_temporal_data")
    private Boolean esTemporalData; // true = datos temporales, false/null = datos de BD
    
    // ==================== CAMPOS PARA DATOS TEMPORALES DE VUELO ====================
    
    @Column(name = "vuelo_codigo_origen", length = 10)
    private String vueloCodigoOrigen; // Código IATA del aeropuerto origen
    
    @Column(name = "vuelo_codigo_destino", length = 10)
    private String vueloCodigoDestino; // Código IATA del aeropuerto destino
    
    @Column(name = "vuelo_ciudad_origen", length = 100)
    private String vueloCiudadOrigen; // Nombre de la ciudad origen
    
    @Column(name = "vuelo_ciudad_destino", length = 100)
    private String vueloCiudadDestino; // Nombre de la ciudad destino
    
    @Column(name = "vuelo_capacidad_maxima")
    private Integer vueloCapacidadMaxima;
    
    @Column(name = "vuelo_hora_salida")
    private LocalTime vueloHoraSalida;
    
    @Column(name = "vuelo_hora_llegada")
    private LocalTime vueloHoraLlegada;
    
    @Column(name = "vuelo_duracion")
    private Double vueloDuracion; // Duración en horas
    
    // ==================== CAMPOS PARA DATOS TEMPORALES DE PEDIDO ====================
    
    @Column(name = "pedido_codigo_origen", length = 10)
    private String pedidoCodigoOrigen; // Código IATA del aeropuerto origen del pedido
    
    @Column(name = "pedido_codigo_destino", length = 10)
    private String pedidoCodigoDestino; // Código IATA del aeropuerto destino del pedido
    
    @Column(name = "pedido_cantidad_productos")
    private Integer pedidoCantidadProductos;
}

