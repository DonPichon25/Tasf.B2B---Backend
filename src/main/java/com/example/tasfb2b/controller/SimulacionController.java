package com.example.tasfb2b.controller;

import com.example.tasfb2b.model.Aeropuerto;
import com.example.tasfb2b.model.Pedido;
import com.example.tasfb2b.model.Solucion;
import com.example.tasfb2b.model.Vuelo;
import com.example.tasfb2b.repository.AeropuertoRepository;
import com.example.tasfb2b.repository.VueloRepository;
import com.example.tasfb2b.service.TabuSearchService;
import com.example.tasfb2b.util.TimeCalculator;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Solucion solucion = tabuSearchService.ejecutarOptimizacion(pedidosHistoricos, pedidosSimulacion, vuelos, aeropuertos, 500);

        // ── 1. capacidadesVuelos: necesario para colorear aviones en el mapa ──
        Map<String, Integer> caps = new HashMap<>();
        for (Vuelo v : vuelos) {
            caps.put(v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida(), v.getCapacidadMax());
        }
        solucion.setCapacidadesVuelos(caps);

        // ── 2. Métricas del panel inferior ──
        Map<String, Aeropuerto> mapaAeros = new HashMap<>();
        for (Aeropuerto a : aeropuertos) mapaAeros.put(a.getCodigo(), a);

        Map<String, Pedido> mapaPedidos = new HashMap<>();
        for (Pedido p : pedidosSimulacion) mapaPedidos.put(p.getIdPedido(), p);

        double totalMinIntra = 0, totalMinInter = 0;
        int countIntra = 0, countInter = 0;
        int exitosos = 0;

        // SLA óptimo: 12h intra (720 min) / 24h inter (1440 min)
        final long SLA_INTRA = 720;
        final long SLA_INTER = 1440;

        for (Map.Entry<String, List<Vuelo>> entry : solucion.getRutasAsignadas().entrySet()) {
            List<Vuelo> ruta = entry.getValue();
            if (ruta == null || ruta.isEmpty()) continue;

            Pedido p = mapaPedidos.get(entry.getKey());
            if (p == null) continue;

            Aeropuerto aOrigen  = mapaAeros.get(p.getOrigen());
            Aeropuerto aDestino = mapaAeros.get(p.getDestino());
            if (aOrigen == null || aDestino == null) continue;

            // Calcular tiempo total de la ruta en minutos
            long totalMin = 0;
            for (int i = 0; i < ruta.size(); i++) {
                Vuelo v = ruta.get(i);
                Aeropuerto vO = mapaAeros.get(v.getOrigen());
                Aeropuerto vD = mapaAeros.get(v.getDestino());
                if (vO != null && vD != null)
                    totalMin += TimeCalculator.calcularDuracionVueloMinutos(v, vO, vD);
                if (i < ruta.size() - 1)
                    totalMin += TimeCalculator.calcularTiempoEsperaMinutos(v, ruta.get(i + 1));
            }
            totalMin += TimeCalculator.TIEMPO_RECOJO_FINAL;

            boolean mismoContinent = aOrigen.getContinente().equals(aDestino.getContinente());
            if (mismoContinent) {
                totalMinIntra += totalMin;
                countIntra++;
                if (totalMin <= SLA_INTRA) exitosos++;
            } else {
                totalMinInter += totalMin;
                countInter++;
                if (totalMin <= SLA_INTER) exitosos++;
            }
        }

        int totalAsignados = countIntra + countInter;
        solucion.setTotalPedidos(totalAsignados);
        solucion.setTasaExito(totalAsignados > 0 ? (exitosos * 100.0 / totalAsignados) : 0);
        solucion.setTiempoPromedioIntra(countIntra > 0 ? (totalMinIntra / countIntra / 60.0) : 0);
        solucion.setTiempoPromedioInter(countInter > 0 ? (totalMinInter / countInter / 60.0) : 0);

        return solucion;
    }
}
