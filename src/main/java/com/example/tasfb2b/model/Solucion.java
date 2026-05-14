package com.example.tasfb2b.model;

import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Solucion {
    private Map<String, List<Vuelo>> rutasAsignadas;
    private Map<String, Integer> ocupacionVuelos;
    private Map<String, Integer> capacidadesVuelos;
    private Map<String, Integer> ocupacionAeropuertos;

    // NUEVAS MÉTRICAS
    private int totalPedidos;
    private double tasaExito; // Porcentaje de pedidos que cumplen SLA
    private double tiempoPromedioIntra;
    private double tiempoPromedioInter;

    private double fitness;

    public Solucion() {
        this.rutasAsignadas = new HashMap<>();
        this.ocupacionVuelos = new HashMap<>();
        this.capacidadesVuelos = new HashMap<>(); // Inicializar
        this.ocupacionAeropuertos = new HashMap<>();
        this.fitness = Double.MAX_VALUE;
    }
}