package com.example.tasfb2b;

import com.example.tasfb2b.model.Aeropuerto;
import com.example.tasfb2b.model.Pedido;
import com.example.tasfb2b.model.Solucion;
import com.example.tasfb2b.model.Vuelo;
import com.example.tasfb2b.service.LectorArchivosService;
import com.example.tasfb2b.service.TabuSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimulacionRunner implements CommandLineRunner {

    private final LectorArchivosService lectorService;
    private final TabuSearchService tabuSearchService;

    @Autowired
    public SimulacionRunner(LectorArchivosService lectorService, TabuSearchService tabuSearchService) {
        this.lectorService = lectorService;
        this.tabuSearchService = tabuSearchService;
    }

    // --- DEFINICIÓN DE LOS 3 ESCENARIOS ---
    public enum TipoEscenario {
        ESCENARIO_1_PRUEBA_RAPIDA,
        ESCENARIO_2_PERIODO_REGULAR,
        ESCENARIO_3_COLAPSO_TOTAL
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("==============================================");
        System.out.println("   INICIANDO SISTEMA DE TRASLADO TASF.B2B     ");
        System.out.println("==============================================");

        // =========================================================================
        // 🛠️ PANEL DE CONTROL: CAMBIA ESTA VARIABLE PARA ELEGIR QUÉ ESCENARIO SIMULAR 🛠️
        // =========================================================================
        TipoEscenario escenarioActual = TipoEscenario.ESCENARIO_1_PRUEBA_RAPIDA;
        // =========================================================================

        // 1. Definir rutas de archivos
        String rutaAeros = "src/main/resources/data/aeropuertos.txt";
        String rutaVuelos = "src/main/resources/data/planesVuelos.txt";
        File carpetaEnvios = new File("src/main/resources/data/envios");

        // 2. Cargar el entorno virtual
        System.out.println("-> Cargando base de datos de aeropuertos...");
        List<Aeropuerto> aeropuertos = lectorService.leerAeropuertos(rutaAeros);

        // --- VERIFICACIÓN DE AEROPUERTOS ---
        System.out.println("\n--- AUDITORÍA COMPLETA DE AEROPUERTOS ---");
        System.out.printf("%-6s | %-20s | %-15s | %-18s | %-4s | %s%n",
                "CÓDIGO", "CIUDAD", "PAÍS", "CONTINENTE", "GMT", "CAPACIDAD");
        System.out.println("-----------------------------------------------------------------------------------------");

        for (Aeropuerto a : aeropuertos) {
            System.out.printf("%-6s | %-20s | %-15s | %-18s | %3d  | %4d%n",
                    a.getCodigo(),
                    a.getNombre(),
                    a.getPais(),
                    a.getContinente(),
                    a.getGmt(),
                    a.getCapacidadMax());
        }
        System.out.println("-----------------------------------------------------------------------------------------\n");

        System.out.println("-> Cargando planes de vuelo...");
        List<Vuelo> vuelos = lectorService.leerVuelos(rutaVuelos);

        System.out.println("-> Cargando demanda de maletas...");
        List<Pedido> pedidosTotales = new ArrayList<>();
        if (carpetaEnvios.exists() && carpetaEnvios.isDirectory()) {
            for (File archivo : carpetaEnvios.listFiles()) {
                if (archivo.getName().endsWith(".txt")) {
                    pedidosTotales.addAll(lectorService.leerEnvios(archivo.getAbsolutePath()));
                }
            }
        } else {
            System.err.println("❌ No se encontró la carpeta de envíos.");
            return;
        }

        System.out.println("-> Ordenando pedidos cronológicamente por fecha de registro...");
        pedidosTotales.sort(java.util.Comparator.comparing(Pedido::getFechaRegistro));

        // 3. APLICAR FILTROS SEGÚN EL ESCENARIO ELEGIDO
        List<Pedido> pedidosAProcesar = new ArrayList<>();
        System.out.println("\n--- CONFIGURANDO " + escenarioActual + " ---");

        switch (escenarioActual) {
            case ESCENARIO_1_PRUEBA_RAPIDA:
                // Toma solo los primeros X pedidos
                int limite = Math.min(25000 , pedidosTotales.size());
                pedidosAProcesar = pedidosTotales.subList(0, limite);
                System.out.println("Filtro: Se procesarán únicamente " + limite + " pedidos.");
                break;

            case ESCENARIO_2_PERIODO_REGULAR:
                // Filtra por fechas:
                LocalDateTime fechaInicio = LocalDateTime.of(2026, 4, 1, 0, 0);
                LocalDateTime fechaFin = LocalDateTime.of(2026, 7, 31, 0, 0);

                pedidosAProcesar = pedidosTotales.stream()
                        .filter(p -> !p.getFechaRegistro().isBefore(fechaInicio) && p.getFechaRegistro().isBefore(fechaFin))
                        .toList();
                System.out.println("Filtro: Pedidos entre " + fechaInicio.toLocalDate() + " y " + fechaFin.toLocalDate());
                break;

            case ESCENARIO_3_COLAPSO_TOTAL:
                // Sin filtros
                pedidosAProcesar = pedidosTotales;
                System.out.println("Filtro: NINGUNO. Advertencia, alto consumo de RAM y tiempo de CPU.");
                break;
        }

        System.out.println("Total de pedidos a procesar en este escenario: " + pedidosAProcesar.size());
        System.out.println("==============================================\n");

        // 4. Disparar la metaheurística
        if (!pedidosAProcesar.isEmpty() && !vuelos.isEmpty() && !aeropuertos.isEmpty()) {

            // --- INICIO DEL CONTADOR DIARIO ---
            System.out.println("\n--- DENSIDAD DE CARGA DIARIA ---");
            if (!pedidosAProcesar.isEmpty()) {
                java.time.LocalDate diaActualTracker = pedidosAProcesar.get(0).getFechaRegistro().toLocalDate();
                int pedidosDelDia = 0;
                int maletasDelDia = 0;

                for (Pedido p : pedidosAProcesar) {
                    java.time.LocalDate diaPedido = p.getFechaRegistro().toLocalDate();
                    if (diaPedido.equals(diaActualTracker)) {
                        pedidosDelDia++;
                        maletasDelDia += p.getCantidadMaletas();
                    } else {
                        System.out.println("Día " + diaActualTracker + " -> Procesando " + pedidosDelDia + " pedidos (" + maletasDelDia + " maletas totales).");
                        diaActualTracker = diaPedido;
                        pedidosDelDia = 1;
                        maletasDelDia = p.getCantidadMaletas();
                    }
                }
                // Imprimir el último día
                System.out.println("Día " + diaActualTracker + " -> Procesando " + pedidosDelDia + " pedidos (" + maletasDelDia + " maletas totales).");
            }
            System.out.println("--------------------------------\n");
            // --- FIN DEL CONTADOR DIARIO ---

            long tiempoInicio = System.currentTimeMillis();

            Solucion mejorSolucion = tabuSearchService.ejecutarOptimizacion(pedidosAProcesar, vuelos, aeropuertos);

            long tiempoFin = System.currentTimeMillis();

            System.out.println("\n==============================================");
            System.out.println("            RESULTADOS DE LA SIMULACIÓN       ");
            System.out.println("==============================================");
            System.out.println("Costo Final (Fitness): " + mejorSolucion.getFitness());
            System.out.println("Tiempo de procesamiento: " + (tiempoFin - tiempoInicio) + " ms");
            imprimirMetricasSLA(mejorSolucion, aeropuertos, pedidosAProcesar);

            // 5. Exportación a TXT Mejorada
            String rutaExportacion = "resultados_" + escenarioActual.name() + ".txt";
            System.out.println("\nExportando el reporte de rutas a: " + rutaExportacion + " ...");

            try (PrintWriter writer = new PrintWriter(new FileWriter(rutaExportacion))) {
                writer.println("==============================================");
                writer.println("REPORTE DE RUTAS - " + escenarioActual);
                writer.println("==============================================");

                if (escenarioActual == TipoEscenario.ESCENARIO_2_PERIODO_REGULAR) {
                    writer.println("PERIODO EVALUADO: 01-04-2026 al 31-07-2026");
                }

                writer.println("Costo Final (Fitness): " + mejorSolucion.getFitness());
                writer.println("Total de pedidos procesados: " + pedidosAProcesar.size());
                writer.println("Tiempo de procesamiento: " + (tiempoFin - tiempoInicio) + " ms");
                writer.println("----------------------------------------------\n");

                // ORDENAR POR ID PARA MAYOR CLARIDAD
                mejorSolucion.getRutasAsignadas().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            List<Vuelo> ruta = entry.getValue();
                            writer.print("Pedido " + entry.getKey() + " (" + ruta.size() + " tramos) -> ");
                            for (Vuelo v : ruta) {
                                writer.print("[" + v.getOrigen() + " a " + v.getDestino() + "] ");
                            }
                            writer.println();
                        });

                System.out.println("¡Exportación completada! El archivo está ordenado por ID.");
            } catch (Exception e) {
                System.err.println("Error al intentar exportar los resultados: " + e.getMessage());
            }

        } else {
            System.err.println("❌ Error: No hay datos para procesar con los filtros actuales.");
        }
    }

    // --- MÉTODO PARA EXPERIMENTACIÓN NUMÉRICA ---
    private void imprimirMetricasSLA(Solucion solucion, List<Aeropuerto> aeropuertos, List<Pedido> pedidosAProcesar) {
        int pedidosLegales = 0;
        int totalPedidos = solucion.getRutasAsignadas().size();
        Integer primerPedidoColapsado = null;

        if (totalPedidos == 0) return;

        // OPTIMIZACIÓN: Mapas de búsqueda rápida
        Map<String, Pedido> mapaPedidos = new java.util.HashMap<>();
        for (Pedido p : pedidosAProcesar) mapaPedidos.put(p.getIdPedido(), p);

        Map<String, Aeropuerto> mapaAeros = new java.util.HashMap<>();
        for (Aeropuerto a : aeropuertos) mapaAeros.put(a.getCodigo(), a);

        // Contadores para el diagnóstico
        int fallasSLA = 0;
        int fallasVuelo = 0;
        int fallasAlmacen = 0;
        Map<String, Integer> cuellosBotella = new java.util.HashMap<>();

        int contadorGlobal = 0;

        for (Map.Entry<String, List<Vuelo>> entry : solucion.getRutasAsignadas().entrySet()) {
            contadorGlobal++;
            String idPedido = entry.getKey();
            List<Vuelo> ruta = entry.getValue();
            if (ruta.isEmpty()) continue;

            Pedido pedidoReal = mapaPedidos.get(idPedido);
            java.time.LocalDateTime tiempoActual = pedidoReal.getFechaRegistro();

            long tiempoRutaMinutos = 0;
            boolean capacidadVuelosOk = true;
            boolean capacidadAlmacenesOk = true;

            for (int i = 0; i < ruta.size(); i++) {
                Vuelo v = ruta.get(i);

                // VALIDACIÓN DE CAPACIDAD DE VUELO
                String idVueloUnico = v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida();
                int maletasEnEsteVuelo = solucion.getOcupacionVuelos().getOrDefault(idVueloUnico, 0);

                if (maletasEnEsteVuelo > v.getCapacidadMax()) {
                    capacidadVuelosOk = false;
                    fallasVuelo++;
                }

                // Uso del mapa optimizado
                Aeropuerto origen = mapaAeros.get(v.getOrigen());
                Aeropuerto destino = mapaAeros.get(v.getDestino());

                if (origen != null && destino != null) {
                    long duracionVuelo = com.example.tasfb2b.util.TimeCalculator.calcularDuracionVueloMinutos(v, origen, destino);
                    tiempoRutaMinutos += duracionVuelo;
                    tiempoActual = tiempoActual.plusMinutes(duracionVuelo);

                    // VALIDACIÓN DE ALMACÉN
                    String diaDestino = tiempoActual.toLocalDate().toString();
                    String keyDestino = destino.getCodigo() + "_" + diaDestino;
                    int maletasEseDia = solucion.getOcupacionAeropuertos().getOrDefault(keyDestino, 0);

                    if (maletasEseDia > destino.getCapacidadMax()) {
                        capacidadAlmacenesOk = false;
                        cuellosBotella.put(destino.getCodigo(), cuellosBotella.getOrDefault(destino.getCodigo(), 0) + 1);
                    }
                }

                if (i < ruta.size() - 1) {
                    long espera = com.example.tasfb2b.util.TimeCalculator.calcularTiempoEsperaMinutos(v, ruta.get(i+1));
                    tiempoRutaMinutos += espera;
                    tiempoActual = tiempoActual.plusMinutes(espera);
                }
            }

            tiempoRutaMinutos += com.example.tasfb2b.util.TimeCalculator.TIEMPO_RECOJO_FINAL;

            Aeropuerto aeroOrigenFinal = mapaAeros.get(ruta.get(0).getOrigen());
            Aeropuerto aeroDestinoFinal = mapaAeros.get(ruta.get(ruta.size() - 1).getDestino());

            boolean cumpleSLA = false;
            if (aeroOrigenFinal != null && aeroDestinoFinal != null) {
                boolean mismoContinente = aeroOrigenFinal.getContinente().equals(aeroDestinoFinal.getContinente());
                long limiteSlaMinutos = mismoContinente ? (24 * 60) : (48 * 60);
                cumpleSLA = tiempoRutaMinutos <= limiteSlaMinutos;
                if (!cumpleSLA) fallasSLA++;
            }

            if (cumpleSLA && capacidadVuelosOk && capacidadAlmacenesOk) {
                pedidosLegales++;
            } else if (primerPedidoColapsado == null) {
                primerPedidoColapsado = contadorGlobal;
            }
        }

        // --- SALIDA ORIGINAL ---
        double porcentaje = ((double) pedidosLegales / totalPedidos) * 100.0;
        double porcentajeReal = Math.floor(porcentaje * 100) / 100.0;

        System.out.println("-> Entregas LEGALES (SLA + Capacidades): " + pedidosLegales + " de " + totalPedidos + " (" + String.format(java.util.Locale.US, "%.2f", porcentajeReal) + "%)");
        System.out.println("-> Entregas fallidas/colapsadas: " + (totalPedidos - pedidosLegales));

        if (primerPedidoColapsado != null) {
            System.out.println("-> [ALERTA] Punto de colapso detectado en el pedido número: " + primerPedidoColapsado);

            // --- NUEVO DIAGNÓSTICO ENTENDIBLE ---
            System.out.println("\n--- DIAGNÓSTICO DE FALLAS ---");
            if (fallasSLA > 0) System.out.println("  * Por SLA (Tiempo excedido): " + fallasSLA);
            if (fallasVuelo > 0) System.out.println("  * Por Vuelos llenos: " + fallasVuelo);
            if (!cuellosBotella.isEmpty()) {
                System.out.println("  * Por Almacenes llenos:");
                cuellosBotella.forEach((k, v) -> System.out.println("    - " + k + ": " + v + " veces."));
            }
        } else {
            System.out.println("-> ¡Excelente! Todos los pedidos cumplieron las reglas.");
        }
    }
}