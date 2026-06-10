package com.example.tasfb2b.service;

import com.example.tasfb2b.model.*;
import com.example.tasfb2b.repository.AeropuertoRepository;
import com.example.tasfb2b.repository.VueloRepository;
import com.example.tasfb2b.util.TimeCalculator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AsyncSimulacionService {

    private final AeropuertoRepository aeropuertoRepository;
    private final VueloRepository vueloRepository;
    private final TabuSearchService tabuSearchService;
    private final JdbcTemplate jdbc;

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

    public AsyncSimulacionService(AeropuertoRepository aeropuertoRepository,
                                  VueloRepository vueloRepository,
                                  TabuSearchService tabuSearchService,
                                  JdbcTemplate jdbc) {
        this.aeropuertoRepository = aeropuertoRepository;
        this.vueloRepository = vueloRepository;
        this.tabuSearchService = tabuSearchService;
        this.jdbc = jdbc;
    }

    // Este método corre en un hilo secundario y no bloquea a Nginx
    @Async
    public void procesarSimulacionEnFondo(String jobId, LocalDateTime inicio, int dias, JobEstado job) {
        try {
            job.setEstado("PROCESANDO");

            List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
            List<Vuelo> vuelos = vueloRepository.findAll();

            // 1. APLICAR LO DEL VIDEO XD (Sa, K, Sc)
            int Sa = 5; // Salto base de 5 minutos
            int K;
            if (dias <= 3) K = 14;
            else if (dias <= 5) K = 24;
            else K = 33; // Para 7 días o más

            int Sc = K * Sa; // Salto de consumo en minutos virtuales (Ej: 70 min)
            int totalMinutosVirtuales = dias * 24 * 60;
            int totalPasos = (int) Math.ceil((double) totalMinutosVirtuales / Sc);

            // 2. CONFIGURAR TIEMPO REAL (Target: 30 minutos reales = 1800 segundos)
            // Calculamos cuánto debe demorar cada paso para estirar la simulación
            long sleepMillis = (1800 / Math.max(1, totalPasos)) * 1000L;

            System.out.println("Iniciando Job " + jobId + " | Días: " + dias + " | Pasos: " + totalPasos + " | Sc: " + Sc + "min");

            // 3. BUCLE DE CONSUMO POR BLOQUES
            for (int paso = 0; paso < totalPasos; paso++) {
                LocalDateTime ventanaInicio = inicio.plusMinutes((long) paso * Sc);
                LocalDateTime ventanaFin = ventanaInicio.plusMinutes(Sc);

                // A. Histórico: Todo lo que pasó antes de esta ventana
                List<Pedido> pedidosHistoricos = jdbc.query(
                        "SELECT id_pedido, origen, destino, fecha_registro, cantidad_maletas, id_cliente " +
                                "FROM pedidos WHERE fecha_registro >= ? AND fecha_registro < ? ORDER BY fecha_registro",
                        PEDIDO_MAPPER, inicio, ventanaInicio); // <-- Ahora usa 'inicio' como límite inferior

                // B. Nuevos: Los pedidos de este bloque Sc específico
                List<Pedido> pedidosNuevos = jdbc.query(
                        "SELECT id_pedido, origen, destino, fecha_registro, cantidad_maletas, id_cliente " +
                                "FROM pedidos WHERE fecha_registro >= ? AND fecha_registro < ? ORDER BY fecha_registro",
                        PEDIDO_MAPPER, ventanaInicio, ventanaFin);

                // C. Ejecutar Algoritmo (Ta)
                Solucion solucionParcial;
                if (pedidosNuevos.isEmpty()) {
                    solucionParcial = tabuSearchService.ejecutarOptimizacion(pedidosHistoricos, List.of(), vuelos, aeropuertos, 0);
                } else {
                    solucionParcial = tabuSearchService.ejecutarOptimizacion(pedidosHistoricos, pedidosNuevos, vuelos, aeropuertos, 20); // Usamos menos iteraciones por paso para control
                }

                // D. Enriquecer la solución con las métricas ACUMULADAS
                List<Pedido> todosLosPedidosHastaAhora = new java.util.ArrayList<>(pedidosHistoricos);
                todosLosPedidosHastaAhora.addAll(pedidosNuevos);
                enriquecerSolucion(solucionParcial, vuelos, aeropuertos, todosLosPedidosHastaAhora);

                // --- FILTROS ESTRICTOS ANTI OUT-OF-MEMORY ---

                // 1. Filtrar rutas: Enviar solo las del bloque actual
                Map<String, List<Vuelo>> rutasFiltradas = solucionParcial.getRutasAsignadas().entrySet().stream()
                        .filter(e -> pedidosNuevos.stream().anyMatch(p -> p.getIdPedido().equals(e.getKey())))
                        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                solucionParcial.setRutasAsignadas(rutasFiltradas);

                // 2. Filtrar capacidadesVuelos (El culpable del peso): Solo enviamos las capacidades de los vuelos que viajan ahora
                java.util.Set<String> vuelosActivos = new java.util.HashSet<>();
                for (List<Vuelo> ruta : rutasFiltradas.values()) {
                    for (Vuelo v : ruta) vuelosActivos.add(v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida());
                }
                solucionParcial.setCapacidadesVuelos(solucionParcial.getCapacidadesVuelos().entrySet().stream()
                        .filter(e -> vuelosActivos.contains(e.getKey()))
                        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

                // 3. Filtrar ocupacionVuelos: Ordenamos de mayor a menor y mandamos top 100
                solucionParcial.setOcupacionVuelos(solucionParcial.getOcupacionVuelos().entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .limit(100)
                        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

                // 4. Filtrar ocupacionAeropuertos: Ordenamos de mayor a menor y mandamos top 100 (para los Cuellos de Botella)
                solucionParcial.setOcupacionAeropuertos(solucionParcial.getOcupacionAeropuertos().entrySet().stream()
                        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                        .limit(100)
                        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

                // E. Actualizar el estado global del Job
                double progreso = ((paso + 1.0) / totalPasos) * 100.0;
                job.setProgreso(progreso);
                job.setSolucionParcial(solucionParcial);

                // F. Estirar el tiempo (El delay visual para llegar a los 30 min reales)
                Thread.sleep(sleepMillis);
            }

            job.setEstado("COMPLETADO");
            job.setProgreso(100.0);
            System.out.println("Job " + jobId + " COMPLETADO.");

        } catch (Exception e) {
            e.printStackTrace();
            job.setEstado("ERROR");
            job.setMensaje(e.getMessage());
        }
    }

    private void enriquecerSolucion(Solucion solucion, List<Vuelo> vuelos, List<Aeropuerto> aeropuertos, List<Pedido> pedidos) {
        Map<String, Integer> caps = new HashMap<>();
        for (Vuelo v : vuelos) caps.put(v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida(), v.getCapacidadMax());
        solucion.setCapacidadesVuelos(caps);

        Map<String, Aeropuerto> mapaAeros = new HashMap<>();
        for (Aeropuerto a : aeropuertos) mapaAeros.put(a.getCodigo(), a);

        Map<String, Pedido> mapaPedidos = new HashMap<>();
        for (Pedido p : pedidos) mapaPedidos.put(p.getIdPedido(), p);

        double totalMinIntra = 0, totalMinInter = 0;
        int countIntra = 0, countInter = 0, exitosos = 0;
        final long SLA_INTRA = 720, SLA_INTER = 1440;

        for (Map.Entry<String, List<Vuelo>> entry : solucion.getRutasAsignadas().entrySet()) {
            List<Vuelo> ruta = entry.getValue();
            if (ruta == null || ruta.isEmpty()) continue;
            Pedido p = mapaPedidos.get(entry.getKey());
            if (p == null) continue;
            Aeropuerto aOrigen  = mapaAeros.get(p.getOrigen());
            Aeropuerto aDestino = mapaAeros.get(p.getDestino());
            if (aOrigen == null || aDestino == null) continue;

            long totalMin = 0;
            for (int i = 0; i < ruta.size(); i++) {
                Vuelo v = ruta.get(i);
                totalMin += TimeCalculator.calcularDuracionVueloMinutos(v, mapaAeros.get(v.getOrigen()), mapaAeros.get(v.getDestino()));
                if (i < ruta.size() - 1) totalMin += TimeCalculator.calcularTiempoEsperaMinutos(v, ruta.get(i + 1));
            }
            totalMin += TimeCalculator.TIEMPO_RECOJO_FINAL;

            if (aOrigen.getContinente().equals(aDestino.getContinente())) {
                totalMinIntra += totalMin; countIntra++; if (totalMin <= SLA_INTRA) exitosos++;
            } else {
                totalMinInter += totalMin; countInter++; if (totalMin <= SLA_INTER) exitosos++;
            }
        }
        int totalAsignados = countIntra + countInter;
        solucion.setTotalPedidos(totalAsignados);
        solucion.setTasaExito(totalAsignados > 0 ? (exitosos * 100.0 / totalAsignados) : 0);
        solucion.setTiempoPromedioIntra(countIntra > 0 ? (totalMinIntra / countIntra / 60.0) : 0);
        solucion.setTiempoPromedioInter(countInter > 0 ? (totalMinInter / countInter / 60.0) : 0);
    }
}