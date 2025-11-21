package com.grupo5e.morapack.core.model;

import com.grupo5e.morapack.core.enums.EstadoProducto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_seq")
    @SequenceGenerator(name = "producto_seq", sequenceName = "productos_id_seq", allocationSize = 50)
    private Integer id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Double peso;

    @Column(nullable = false)
    private Double volumen;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    // Relación: muchos productos pertenecen a un pedido
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    private EstadoProducto estado;

    /**
     * ID de la instancia de vuelo asignada a este producto.
     * Formato: "FL-{vueloId}-DAY-{day}-{HHmm}"
     * Ejemplo: "FL-45-DAY-0-0800"
     * 
     * Permite tracking específico de qué salida diaria transporta este producto.
     */
    @Column(name = "instancia_vuelo_asignada", length = 50)
    private String instanciaVueloAsignada;

    /**
     * Tiempo de llegada estimado o real del producto al destino final.
     * Usado para calcular tiempos de entrega y actualizar estados.
     */
    @Column(name = "fecha_hora_llegada")
    private LocalDateTime fechaHoraLlegada;
}
