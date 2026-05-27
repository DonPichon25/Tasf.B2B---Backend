package com.example.tasfb2b.model;

import lombok.Data;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Solucion {
    private Map<String, List<Vuelo>> rutasAsignadas;
    private Map<String, Integer> ocupacionVuelos;
    private Map<String, Integer> ocupacionAeropuertos;

    // Campos que necesita el frontend para renderizar aviones y métricas
    private Map<String, Integer> capacidadesVuelos;   // "LIM-BOG-08:00" → capacidadMax
    private int totalPedidos;
    private double tasaExito;
    private double tiempoPromedioIntra;
    private double tiempoPromedioInter;

    private double fitness;

    public Solucion() {
        this.rutasAsignadas = new HashMap<>();
        this.ocupacionVuelos = new HashMap<>();
        this.ocupacionAeropuertos = new HashMap<>();
        this.capacidadesVuelos = new HashMap<>();
        this.fitness = Double.MAX_VALUE;
    }

    public Solucion clonar() {
        Solucion copia = new Solucion();
        copia.setFitness(this.fitness);
        copia.setRutasAsignadas(new HashMap<>(this.rutasAsignadas));
        copia.setOcupacionVuelos(new HashMap<>(this.ocupacionVuelos));
        // Clonamos la memoria de los aeropuertos para que los vecinos no pisen los datos
        copia.setOcupacionAeropuertos(new HashMap<>(this.ocupacionAeropuertos));
        return copia;
    }
}