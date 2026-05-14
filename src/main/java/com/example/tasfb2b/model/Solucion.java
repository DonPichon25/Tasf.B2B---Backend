package com.example.tasfb2b.model;

import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Solucion {
    private Map<String, List<Vuelo>> rutasAsignadas;
    private Map<String, Integer> ocupacionVuelos;

    // NUEVO: Registro contable diario de los aeropuertos.
    // Guardará llaves como "MAD_2026-04-14" -> 450 maletas
    private Map<String, Integer> ocupacionAeropuertos;

    private double fitness;

    public Solucion() {
        this.rutasAsignadas = new HashMap<>();
        this.ocupacionVuelos = new HashMap<>();
        this.ocupacionAeropuertos = new HashMap<>();
        this.fitness = Double.MAX_VALUE;
    }
}