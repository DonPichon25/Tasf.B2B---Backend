package com.example.tasfb2b.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok genera Getters, Setters y toString automáticamente
@AllArgsConstructor
@NoArgsConstructor
public class Aeropuerto {
    private String codigo;        // Ej. SKBO
    private String nombre;        // Ej. Bogota
    private String pais;          // Ej. Colombia
    private String continente;    // Ej. America del Sur
    private int gmt;              // Ej. -5
    private int capacidadMax;     // Ej. 430
    private double latitud;       // En formato decimal para React luego (opcional por ahora)
    private double longitud;

    // Capacidad dinámica durante la simulación
    private int maletasActuales = 0;
}