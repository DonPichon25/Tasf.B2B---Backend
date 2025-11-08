package com.grupo5e.morapack.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una solución completa generada por el algoritmo ALNS.
 * Persiste los resultados de una ejecución del algoritmo con todas sus métricas.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "soluciones")
public class Solucion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;
    
    /**
     * Relación: Una solución contiene múltiples rutas.
     * Cascade ALL: Al eliminar una solución, se eliminan sus rutas.
     * orphanRemoval: Si una ruta se desvincula, se elimina automáticamente.
     */
    @OneToMany(mappedBy = "solucion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default    
    private List<Ruta> rutas = new ArrayList<>();
    
    /**
     * Costo total de la solución (suma de costos de todas las rutas).
     */
    @Column(name = "costo_total", nullable = false)
    private Double costoTotal;
    
    /**
     * Tiempo total de la solución en horas (suma de tiempos de todas las rutas).
     */
    @Column(name = "tiempo_total", nullable = false)
    private Double tiempoTotal;
    
    /**
     * Número de paquetes que no pudieron ser entregados en esta solución.
     * Un valor bajo indica mejor solución.
     */
    @Column(name = "paquetes_no_entregados", nullable = false)
    private Integer paquetesNoEntregados;
    
    /**
     * Fitness de la solución (métrica de calidad global).
     * Mayor fitness = mejor solución.
     * Típicamente calculado como: fitness = peso_costo * (1/costo) + peso_tiempo * (1/tiempo) - peso_no_entregados * noEntregados
     */
    @Column(name = "fitness", nullable = false)
    private Double fitness;
    
    /**
     * Nombre del algoritmo usado para generar esta solución.
     * Para nuestro caso siempre será "ALNS".
     */
    @Column(name = "algoritmo_usado", nullable = false, length = 50)
    private String algoritmoUsado;
    
    /**
     * Número total de iteraciones ejecutadas por el algoritmo.
     */
    @Column(name = "iteraciones_ejecutadas")
    private Integer iteracionesEjecutadas;
    
    /**
     * Tiempo de ejecución del algoritmo en segundos.
     */
    @Column(name = "tiempo_ejecucion_segundos")
    private Long tiempoEjecucionSegundos;
    
    /**
     * Temperatura final del algoritmo (para análisis).
     */
    @Column(name = "temperatura_final")
    private Double temperaturaFinal;
    
    /**
     * Número de pedidos procesados en esta ejecución.
     */
    @Column(name = "total_pedidos")
    private Integer totalPedidos;
    
    /**
     * Número de rutas generadas.
     */
    @Column(name = "total_rutas")
    private Integer totalRutas;
    
    /**
     * Capacidad total utilizada en almacenes.
     */
    @Column(name = "capacidad_almacenes_usada")
    private Integer capacidadAlmacenesUsada;
    
    /**
     * Observaciones o notas sobre esta solución (opcional).
     */
    @Column(name = "observaciones", length = 1000)
    private String observaciones;
    
    /**
     * Versión del dataset usado (para tracking).
     */
    @Column(name = "version_datos", length = 50)
    private String versionDatos;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Método de conveniencia para agregar una ruta a la solución.
     */
    public void agregarRuta(Ruta ruta) {
        rutas.add(ruta);
        ruta.setSolucion(this);
    }
    
    /**
     * Método de conveniencia para remover una ruta de la solución.
     */
    public void removerRuta(Ruta ruta) {
        rutas.remove(ruta);
        ruta.setSolucion(null);
    }
    
    /**
     * Calcula el porcentaje de pedidos entregados exitosamente.
     */
    public double calcularPorcentajeEntregados() {
        if (totalPedidos == null || totalPedidos == 0) {
            return 0.0;
        }
        int entregados = totalPedidos - (paquetesNoEntregados != null ? paquetesNoEntregados : 0);
        return (entregados * 100.0) / totalPedidos;
    }
    
    /**
     * Calcula el costo promedio por ruta.
     */
    public double calcularCostoPromedioRuta() {
        if (totalRutas == null || totalRutas == 0 || costoTotal == null) {
            return 0.0;
        }
        return costoTotal / totalRutas;
    }
    
    /**
     * Calcula el tiempo promedio por ruta en horas.
     */
    public double calcularTiempoPromedioRuta() {
        if (totalRutas == null || totalRutas == 0 || tiempoTotal == null) {
            return 0.0;
        }
        return tiempoTotal / totalRutas;
    }
}

