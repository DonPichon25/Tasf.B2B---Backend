package com.example.tasfb2b.controller;

import com.example.tasfb2b.model.Aeropuerto;
import com.example.tasfb2b.model.Pedido;
import com.example.tasfb2b.model.Solucion;
import com.example.tasfb2b.model.Vuelo;
import com.example.tasfb2b.repository.AeropuertoRepository;
import com.example.tasfb2b.repository.VueloRepository;
import com.example.tasfb2b.service.TabuSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SimulacionController {

    private final AeropuertoRepository aeropuertoRepository;
    private final VueloRepository vueloRepository;
    private final TabuSearchService tabuSearchService;
    private final JdbcTemplate jdbc;

    // RowMapper reutilizable: convierte cada fila SQL en un objeto Pedido sin tracking JPA
    private static final RowMapper<Pedido> PEDIDO_MAPPER = (rs, rowNum) -> {
        Pedido p = new Pedido();
        p.setIdPedido(rs.getString("id_pedido"));
        p.setOrigen(rs.getString("origen"));
        p.setDestino(rs.getString("destino"));
        p.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        p.setCantidadMaletas(rs.getInt("cantidad_maletas"));
        p.setIdCliente(rs.getString("id_cliente"));
        return p;
    };

    public SimulacionController(AeropuertoRepository aeropuertoRepository,
                                VueloRepository vueloRepository,
                                TabuSearchService tabuSearchService,
                                JdbcTemplate jdbc) {
        this.aeropuertoRepository = aeropuertoRepository;
        this.vueloRepository = vueloRepository;
        this.tabuSearchService = tabuSearchService;
        this.jdbc = jdbc;
    }

    @GetMapping("/simulacion")
    public Solucion ejecutarSimulacion(
            @RequestParam(name = "fechaInicio") String fechaInicio,
            @RequestParam(name = "dias") int dias
    ) {
        LocalDateTime inicio = LocalDateTime.parse(fechaInicio);
        LocalDateTime fin = inicio.plusDays(dias);

        // Aeropuertos y vuelos son pequeños: JPA está bien
        List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
        List<Vuelo> vuelos = vueloRepository.findAll();

        if (aeropuertos.isEmpty() || vuelos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Faltan datos en la BD. Sube aeropuertos.txt y planesVuelos.txt desde Cargar Datos.");
        }

        // Pedidos: se usa JdbcTemplate con filtro de fechas para evitar cargar los 9M en memoria
        List<Pedido> pedidosHistoricos = jdbc.query(
                "SELECT id_pedido, origen, destino, fecha_registro, cantidad_maletas, id_cliente " +
                "FROM pedidos WHERE fecha_registro < ? ORDER BY fecha_registro",
                PEDIDO_MAPPER, inicio);

        List<Pedido> pedidosSimulacion = jdbc.query(
                "SELECT id_pedido, origen, destino, fecha_registro, cantidad_maletas, id_cliente " +
                "FROM pedidos WHERE fecha_registro >= ? AND fecha_registro < ? ORDER BY fecha_registro",
                PEDIDO_MAPPER, inicio, fin);

        if (pedidosSimulacion.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No hay pedidos en el periodo seleccionado (" + inicio + " → " + fin + ").");
        }

        return tabuSearchService.ejecutarOptimizacion(pedidosHistoricos, pedidosSimulacion, vuelos, aeropuertos);
    }
}
