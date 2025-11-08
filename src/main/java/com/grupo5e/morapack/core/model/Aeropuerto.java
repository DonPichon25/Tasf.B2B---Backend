package com.grupo5e.morapack.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.grupo5e.morapack.core.enums.EstadoAeropuerto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "aeropuertos")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Aeropuerto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Código IATA
    @Column(nullable = false, unique = true, length = 8)
    private String codigoIATA;

    @Column(length = 120)
    private String alias;

    private Integer zonaHorariaUTC;
    private String latitud;
    private String longitud;

    // Relación con Ciudad (muchos aeropuertos pueden estar en una ciudad)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciudad_id", nullable = false)
    private Ciudad ciudad;

    @Enumerated(EnumType.STRING)
    private EstadoAeropuerto estado;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;
    
    // Métodos de conveniencia para compatibilidad con código existente
    // Delegan al Almacen asociado
    
    public Integer getCapacidadMaxima() {
        return almacen != null ? almacen.getCapacidadMaxima() : 0;
    }
    
    public Integer getCapacidadActual() {
        return almacen != null ? almacen.getCapacidadUsada() : 0;
    }
    
    public void setCapacidadActual(Integer capacidad) {
        if (almacen != null) {
            almacen.setCapacidadUsada(capacidad);
        }
    }
    
    public void setCapacidadMaxima(Integer capacidad) {
        if (almacen != null) {
            almacen.setCapacidadMaxima(capacidad);
        }
    }
}
