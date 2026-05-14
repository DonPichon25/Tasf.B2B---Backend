package com.example.tasfb2b.controller;

import com.example.tasfb2b.model.Aeropuerto;
import com.example.tasfb2b.model.Pedido;
import com.example.tasfb2b.model.Solucion;
import com.example.tasfb2b.model.Vuelo;
import com.example.tasfb2b.service.LectorArchivosService;
import com.example.tasfb2b.service.TabuSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api") // Todas las rutas empezarán con /api
@CrossOrigin(origins = "*") // Esto permite que tu React se conecte sin errores de seguridad (CORS)
public class SimulacionController {

    private final LectorArchivosService lectorService;
    private final TabuSearchService tabuSearchService;

    @Autowired
    public SimulacionController(LectorArchivosService lectorService, TabuSearchService tabuSearchService) {
        this.lectorService = lectorService;
        this.tabuSearchService = tabuSearchService;
    }

    @GetMapping("/simulacion")
    public Solucion ejecutarSimulacion(
            @RequestParam(name = "fechaInicio") String fechaInicio, // Añadido el nombre explícito
            @RequestParam(name = "dias") int dias                  // Añadido el nombre explícito
    ) {
        // 1. Convertir la fecha que viene del Front
        LocalDateTime inicio = LocalDateTime.parse(fechaInicio);
        LocalDateTime fin = inicio.plusDays(dias);

        // 2. Cargar datos (Rutas de archivos que ya usas)
        String rutaAeros = "src/main/resources/data/aeropuertos.txt";
        String rutaVuelos = "src/main/resources/data/planesVuelos.txt";
        File carpetaEnvios = new File("src/main/resources/data/envios");

        List<Aeropuerto> aeropuertos = lectorService.leerAeropuertos(rutaAeros);
        List<Vuelo> vuelos = lectorService.leerVuelos(rutaVuelos);

        List<Pedido> pedidosTotales = new ArrayList<>();
        for (File archivo : carpetaEnvios.listFiles()) {
            if (archivo.getName().endsWith(".txt")) {
                pedidosTotales.addAll(lectorService.leerEnvios(archivo.getAbsolutePath()));
            }
        }
        pedidosTotales.sort(java.util.Comparator.comparing(Pedido::getFechaRegistro));

        // 3. Separar Historia vs Simulación (Tu lógica de "Warm-up")
        List<Pedido> pedidosHistoricos = pedidosTotales.stream()
                .filter(p -> p.getFechaRegistro().isBefore(inicio))
                .toList();

        List<Pedido> pedidosSimulacion = pedidosTotales.stream()
                .filter(p -> !p.getFechaRegistro().isBefore(inicio) && p.getFechaRegistro().isBefore(fin))
                .toList();

        // 4. Ejecutar Algoritmo
        // Spring Boot automáticamente convertirá el objeto 'Solucion' a JSON para el Front
        return tabuSearchService.ejecutarOptimizacion(pedidosHistoricos, pedidosSimulacion, vuelos, aeropuertos);
    }
}