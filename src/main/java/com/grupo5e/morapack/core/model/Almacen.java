package com.grupo5e.morapack.core.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "almacenes")
public class Almacen {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false)
    private String nombre;
    
    @Column(nullable = false)
    private Integer capacidadMaxima;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer capacidadUsada = 0;
    
    @Builder.Default
    private Boolean esAlmacenPrincipal = false;
    
    @OneToOne(mappedBy = "almacen")
    private Aeropuerto aeropuerto;

    @Column(nullable = true)
    private int tipoData;
}

