package com.example.tasfb2b.service;

import com.example.tasfb2b.model.Aeropuerto;
import com.example.tasfb2b.model.Pedido;
import com.example.tasfb2b.model.Solucion;
import com.example.tasfb2b.model.Vuelo;
import com.example.tasfb2b.util.TimeCalculator;
import org.springframework.stereotype.Service;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Queue;

import java.util.*;

@Service
public class TabuSearchService {

    private static final int ITERACIONES_MAXIMAS = 500;
    private static final int TABU_TENURE = 30;
    private static final int MAX_SIN_MEJORA = 200;

    // --- Cache de Rutas Maestras ---
    private final Map<String, List<Vuelo>> cacheRutasMaestras = new HashMap<>();

    public Solucion ejecutarOptimizacion(List<Pedido> pedidos, List<Vuelo> vuelosTotales, List<Aeropuerto> aeropuertos) {
        // Limpiamos la cache al iniciar cada simulación
        cacheRutasMaestras.clear();

        System.out.println("Iniciando Búsqueda Tabú para " + pedidos.size() + " pedidos...");

        // Convertimos Listas a Mapas para búsquedas instantáneas
        Map<String, Aeropuerto> mapaAeros = new HashMap<>();
        for (Aeropuerto a : aeropuertos) {
            mapaAeros.put(a.getCodigo(), a);
        }

        if (mapaAeros.isEmpty()) {
            System.err.println("CRÍTICO: El mapa de aeropuertos está vacío. Se detiene la simulación para evitar colapso.");
            return new Solucion();
        }

        Map<String, Vuelo> mapaVuelos = new HashMap<>();
        for (Vuelo v : vuelosTotales) {
            String id = v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida();
            mapaVuelos.put(id, v);
        }

        // 1. Generar Solución Inicial (Arranque Voraz)
        Solucion solucionActual = generarSolucionInicialVoraz(pedidos, vuelosTotales, mapaAeros);
        solucionActual.setFitness(evaluarFitness(solucionActual, pedidos, mapaAeros, mapaVuelos));

        Solucion mejorSolucionGlobal = solucionActual.clonar();

        // Memoria Tabú: Guarda el "IDPedido-RutaNueva" para no revertir el cambio pronto
        Map<String, Integer> listaTabu = new HashMap<>();

        int iteracionesSinMejora = 0;

        // 2. Bucle Principal
        for (int iter = 0; iter < ITERACIONES_MAXIMAS; iter++) {
            if (iteracionesSinMejora >= MAX_SIN_MEJORA) {
                System.out.println("Parada temprana: No hay mejoras tras " + MAX_SIN_MEJORA + " iteraciones.");
                break;
            }

            // A. Generar vecindario (variaciones de la solución actual)
            List<Solucion> vecindario = generarVecindario(solucionActual, pedidos, vuelosTotales, mapaAeros, mapaVuelos);

            Solucion mejorVecino = null;
            String mejorMovimientoAtributo = "";

            // B. Evaluar vecinos
            for (Solucion vecino : vecindario) {

                // Extraer el identificador del movimiento que generó este vecino (lógica a implementar)
                String atributoMovimiento = obtenerAtributoMovimiento(solucionActual, vecino);

                boolean esTabu = listaTabu.containsKey(atributoMovimiento);
                boolean cumpleAspiracion = vecino.getFitness() < mejorSolucionGlobal.getFitness();

                if (!esTabu || cumpleAspiracion) {
                    if (mejorVecino == null || vecino.getFitness() < mejorVecino.getFitness()) {
                        mejorVecino = vecino;
                        mejorMovimientoAtributo = atributoMovimiento;
                    }
                }
            }

            if (mejorVecino == null) break; // No hubo vecinos admisibles

            // C. Aplicar el mejor movimiento admisible
            solucionActual = mejorVecino;

            // D. Actualizar Lista Tabú
            actualizarListaTabu(listaTabu);
            listaTabu.put(mejorMovimientoAtributo, TABU_TENURE);

            // E. Actualizar récord global
            if (solucionActual.getFitness() < mejorSolucionGlobal.getFitness()) {
                mejorSolucionGlobal = solucionActual.clonar();
                iteracionesSinMejora = 0;
                System.out.println("Iter " + iter + " | Nuevo mejor Fitness: " + mejorSolucionGlobal.getFitness());
            } else {
                iteracionesSinMejora++;
            }
        }

        System.out.println("Optimización finalizada.");
        realizarDiagnosticoColapso(mejorSolucionGlobal, pedidos, mapaAeros, mapaVuelos);
        return mejorSolucionGlobal;
    }

    // ============================
    // FUNCIONES AUXILIARES
    // ============================

    private Solucion generarSolucionInicialVoraz(List<Pedido> pedidos, List<Vuelo> vuelosTotales, Map<String, Aeropuerto> mapaAeros) {
        Solucion solInicial = new Solucion();

        Map<String, List<Vuelo>> vuelosPorOrigen = new HashMap<>();
        for (Vuelo v : vuelosTotales) {
            vuelosPorOrigen.computeIfAbsent(v.getOrigen(), k -> new ArrayList<>()).add(v);
        }

        for (Pedido pedido : pedidos) {
            String llaveCache = pedido.getOrigen() + "-" + pedido.getDestino();

            // Si la ruta no está en cache, el BFS la busca una sola vez
            List<Vuelo> rutaAsignada = cacheRutasMaestras.computeIfAbsent(llaveCache,
                    k -> buscarRutaBFS(pedido.getOrigen(), pedido.getDestino(), vuelosPorOrigen));

            if (!rutaAsignada.isEmpty()) {
                // Importante: Guardamos una copia (new ArrayList) para que cada pedido sea independiente
                solInicial.getRutasAsignadas().put(pedido.getIdPedido(), new ArrayList<>(rutaAsignada));
                registrarImpactoAeropuertos(solInicial, pedido, rutaAsignada, 1, mapaAeros);
            }
        }
        return solInicial;
    }

    /**
     * Algoritmo de Búsqueda en Anchura (BFS) para encontrar la ruta con menos escalas
     */
    private List<Vuelo> buscarRutaBFS(String origen, String destino, Map<String, List<Vuelo>> vuelosPorOrigen) {
        Queue<List<Vuelo>> cola = new LinkedList<>();
        Set<String> visitados = new HashSet<>();

        // Arrancamos colocando en la cola todos los vuelos directos que salen del origen
        if (vuelosPorOrigen.containsKey(origen)) {
            for (Vuelo v : vuelosPorOrigen.get(origen)) {
                List<Vuelo> rutaInicial = new ArrayList<>();
                rutaInicial.add(v);
                cola.add(rutaInicial);
            }
            visitados.add(origen);
        }

        // Exploramos el grafo de conexiones
        while (!cola.isEmpty()) {
            List<Vuelo> rutaActual = cola.poll();
            Vuelo ultimoVuelo = rutaActual.get(rutaActual.size() - 1);
            String aeropuertoActual = ultimoVuelo.getDestino();

            // ¿Llegamos al destino final?
            if (aeropuertoActual.equals(destino)) {
                return rutaActual;
            }

            // Si no hemos llegado, buscamos los vuelos que salen de esta escala
            if (!visitados.contains(aeropuertoActual) && vuelosPorOrigen.containsKey(aeropuertoActual)) {
                visitados.add(aeropuertoActual);

                // --- MAGIA PURA: Mezclamos las conexiones para balancear la carga global ---
                List<Vuelo> opciones = new ArrayList<>(vuelosPorOrigen.get(aeropuertoActual));

                // ORDENAR POR HORA DE LLEGADA: Garantiza que la primera ruta hallada sea la más veloz
                opciones.sort(Comparator.comparing(Vuelo::getHoraLlegada));

                for (Vuelo conexion : opciones) {
                    if (TimeCalculator.esConexionFisicamentePosible(ultimoVuelo, conexion)) {
                        long espera = TimeCalculator.calcularTiempoEsperaMinutos(ultimoVuelo, conexion);
                        if (espera <= 18 * 60) {
                            List<Vuelo> nuevaRuta = new ArrayList<>(rutaActual);
                            nuevaRuta.add(conexion);
                            cola.add(nuevaRuta);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(); // Retorna lista vacía si es imposible conectar los puntos
    }

    private double evaluarFitness(Solucion solucion, List<Pedido> pedidos, Map<String, Aeropuerto> mapaAeros, Map<String, Vuelo> mapaVuelos) {

        double costoTotal = 0.0;

        // --- 1. EVALUAR TIEMPOS Y SLA POR CADA PEDIDO ---
        for (Pedido pedido : pedidos) {
            List<Vuelo> ruta = solucion.getRutasAsignadas().get(pedido.getIdPedido());

            // Castigo si la maleta se quedó sin ruta asignada
            if (ruta == null || ruta.isEmpty()) {
                costoTotal += 999999.0;
                continue;
            }

            long tiempoTotalRutaMinutos = 0;
            Aeropuerto origenPedido = mapaAeros.get(pedido.getOrigen());
            Aeropuerto destinoPedido = mapaAeros.get(pedido.getDestino());

            // Validar que la ruta termine en el destino correcto (Evita rutas de un solo tramo erróneas)
            Vuelo ultimoVuelo = ruta.get(ruta.size() - 1);
            if (!ultimoVuelo.getDestino().equals(pedido.getDestino())) {
                costoTotal += 999999.0;
            }

            for (int i = 0; i < ruta.size(); i++) {
                Vuelo vueloActual = ruta.get(i);

                // --- MODO DEBUG: CAZADOR DE NULOS ---
                String codOrigen = vueloActual.getOrigen();
                String codDestino = vueloActual.getDestino();

                Aeropuerto origenVuelo = mapaAeros.get(codOrigen);
                Aeropuerto destinoVuelo = mapaAeros.get(codDestino);

                if (origenVuelo == null) {
                    System.err.println("\n[DEBUG FATAL] El vuelo intenta salir de '" + codOrigen + "', pero no existe en aeropuertos.txt!");
                    System.err.println("Longitud del string problemático: " + codOrigen.length());
                    System.exit(1); // Apagamos el sistema de golpe para ver el log
                }
                if (destinoVuelo == null) {
                    System.err.println("\n[DEBUG FATAL] El vuelo intenta llegar a '" + codDestino + "', pero no existe en aeropuertos.txt!");
                    System.err.println("Longitud del string problemático: " + codDestino.length());
                    System.exit(1);
                }
                // -----------------------------------

                // A. Sumar tiempo de vuelo...
                tiempoTotalRutaMinutos += TimeCalculator.calcularDuracionVueloMinutos(vueloActual, origenVuelo, destinoVuelo);

                // B. Sumar tiempo de espera y validar regla de 10 minutos
                if (i < ruta.size() - 1) {
                    Vuelo vueloSiguiente = ruta.get(i + 1);

                    // Si la escala es menor a 10 minutos, es físicamente imposible
                    if (!TimeCalculator.esConexionFisicamentePosible(vueloActual, vueloSiguiente)) {
                        costoTotal += 999999.0; // Castigo letal: Esta ruta no sirve
                        break;
                    }

                    tiempoTotalRutaMinutos += TimeCalculator.calcularTiempoEsperaMinutos(vueloActual, vueloSiguiente);
                }
            }

            // Regla 6: Sumar 10 minutos desde que llega al almacén hasta que el cliente la recoge
            tiempoTotalRutaMinutos += TimeCalculator.TIEMPO_RECOJO_FINAL;

            // Sumar el tiempo base al costo (El algoritmo siempre buscará la ruta más corta)
            costoTotal += tiempoTotalRutaMinutos;

            // C. Penalización por SLA (1 día intra, 2 días inter)
            boolean esMismoContinente = origenPedido.getContinente().equals(destinoPedido.getContinente());
            costoTotal += TimeCalculator.calcularPenalizacionTiempo(tiempoTotalRutaMinutos, esMismoContinente);
        }

        // --- 2. EVALUAR CAPACIDADES DE VUELOS ---
        for (Map.Entry<String, Integer> entry : solucion.getOcupacionVuelos().entrySet()) {
            String idVueloUnico = entry.getKey();
            int maletasAsignadas = entry.getValue();

            // Cortamos la fecha para buscar en el mapa original (ej. separamos "SKBO-LIM-14:30" de "2026-01-02")
            String idVueloBase = idVueloUnico.split("_")[0];

            Vuelo vueloReal = mapaVuelos.get(idVueloBase);
            if (vueloReal != null && maletasAsignadas > vueloReal.getCapacidadMax()) {
                int exceso = maletasAsignadas - vueloReal.getCapacidadMax();
                costoTotal += (exceso * 5000.0);
            }
        }

        // --- 3. NUEVO: EVALUAR CAPACIDADES DIARIAS DE AEROPUERTOS ---
        for (Map.Entry<String, Integer> entry : solucion.getOcupacionAeropuertos().entrySet()) {
            String[] partes = entry.getKey().split("_"); // Rompe "MAD_2026-04-14"
            String codAero = partes[0];
            int maletasEsaHora = entry.getValue();

            Aeropuerto aeroReal = mapaAeros.get(codAero);
            if (aeroReal != null && maletasEsaHora > aeroReal.getCapacidadMax()) {
                // Si en ese día entraron más maletas del límite físico (ej. 500)
                int exceso = maletasEsaHora - aeroReal.getCapacidadMax();
                costoTotal += (exceso * 5000.0); // Castigo letal al fitness
            }
        }

        return costoTotal;
    }

    private List<Solucion> generarVecindario(Solucion actual, List<Pedido> pedidos, List<Vuelo> vuelosTotales, Map<String, Aeropuerto> mapaAeros, Map<String, Vuelo> mapaVuelos) {
        List<Solucion> vecinos = new ArrayList<>();
        Random random = new Random();

        // 1. Elegimos un subconjunto de pedidos al azar para intentar re-rutearlos
        // (No evaluamos todos los miles de pedidos porque el algoritmo nunca terminaría)
        int numCandidatos = Math.min(50, pedidos.size());
        List<Pedido> pedidosMezclados = new ArrayList<>(pedidos);
        Collections.shuffle(pedidosMezclados, random);
        List<Pedido> candidatos = pedidosMezclados.subList(0, numCandidatos);

        // Agrupamos vuelos para el buscador alternativo
        Map<String, List<Vuelo>> vuelosPorOrigen = new HashMap<>();
        for (Vuelo v : vuelosTotales) {
            vuelosPorOrigen.computeIfAbsent(v.getOrigen(), k -> new ArrayList<>()).add(v);
        }

        // 2. Para cada candidato, buscamos una ruta alternativa
        for (Pedido pedido : candidatos) {
            List<Vuelo> rutaAntigua = actual.getRutasAsignadas().get(pedido.getIdPedido());
            if (rutaAntigua == null || rutaAntigua.isEmpty()) continue;

            // Para obligar al BFS a buscar una ruta DIFERENTE, "prohibimos" temporalmente
            // el primer vuelo que estaba usando en la ruta antigua.
            Vuelo vueloAProhibir = rutaAntigua.get(0);

            List<Vuelo> rutaAlternativa = buscarRutaAlternativaBFS(
                    pedido.getOrigen(),
                    pedido.getDestino(),
                    vuelosPorOrigen,
                    vueloAProhibir
            );

            // Si encontramos un camino distinto, creamos un "Vecino" (nuevo tablero de juego)
            if (!rutaAlternativa.isEmpty() && !rutaAlternativa.equals(rutaAntigua)) {
                Solucion vecino = actual.clonar();

                // 1. Delta de Tiempo y SLA
                double costoViejo = calcularCostoRutaUnica(pedido, rutaAntigua, mapaAeros);
                double costoNuevo = calcularCostoRutaUnica(pedido, rutaAlternativa, mapaAeros);

                // 2. Actualizar ocupación (Mapas)
                registrarImpactoAeropuertos(vecino, pedido, rutaAntigua, -1, mapaAeros);
                registrarImpactoAeropuertos(vecino, pedido, rutaAlternativa, 1, mapaAeros);

                // 3. Delta de Penalizaciones (Solo chequeamos si las capacidades se rompieron)
                double nuevoFitness = actual.getFitness() - costoViejo + costoNuevo;

                // OPCIONAL: Si quieres precisión total en capacidades sin re-evaluar todo:
                nuevoFitness += calcularDeltaPenalizaciones(actual, vecino, pedido, rutaAntigua, rutaAlternativa, mapaVuelos, mapaAeros);

                vecino.setFitness(nuevoFitness);
                vecino.getRutasAsignadas().put(pedido.getIdPedido(), rutaAlternativa);
                vecinos.add(vecino);
            }
        }

        return vecinos;
    }

    private double calcularDeltaPenalizaciones(Solucion actual, Solucion vecino, Pedido pedido, List<Vuelo> rutaAntigua, List<Vuelo> rutaNueva, Map<String, Vuelo> mapaVuelos, Map<String, Aeropuerto> mapaAeros) {
        double deltaPenalidad = 0.0;
        java.time.LocalDateTime tiempoActual = pedido.getFechaRegistro();
        int cantidad = pedido.getCantidadMaletas();

        // 1. Evaluar el impacto en la Ruta Antigua (en la solución 'actual')
        // ¿Quitando esta ruta nos libramos de alguna multa que estábamos pagando?
        for (Vuelo v : rutaAntigua) {
            String diaVuelo = tiempoActual.toLocalDate().toString();
            String idVueloUnico = v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida() + "_" + diaVuelo;

            int ocupacionAntes = actual.getOcupacionVuelos().getOrDefault(idVueloUnico, 0);
            int limiteAvion = mapaVuelos.get(v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida()).getCapacidadMax();

            // Si el avión estaba excedido, al quitar estas maletas, reducimos la multa
            if (ocupacionAntes > limiteAvion) {
                int excesoAntes = ocupacionAntes - limiteAvion;
                int excesoDespues = Math.max(0, (ocupacionAntes - cantidad) - limiteAvion);
                deltaPenalidad -= ((excesoAntes - excesoDespues) * 5000.0); // Restamos la multa aliviada
            }

            // (Simplificamos omitiendo el tiempo exacto de almacenes para mantener O(1) ultrarrápido,
            //  los aviones suelen ser el cuello de botella principal)
        }

        // 2. Evaluar el impacto en la Ruta Nueva (en la solución 'vecino')
        // ¿Meter maletas en esta nueva ruta nos genera una nueva multa?
        tiempoActual = pedido.getFechaRegistro();
        for (Vuelo v : rutaNueva) {
            String diaVuelo = tiempoActual.toLocalDate().toString();
            String idVueloUnico = v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida() + "_" + diaVuelo;

            int ocupacionDespues = vecino.getOcupacionVuelos().getOrDefault(idVueloUnico, 0);
            int limiteAvion = mapaVuelos.get(v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida()).getCapacidadMax();

            // Si la nueva ruta sobrepasa el límite del avión, sumamos la multa
            if (ocupacionDespues > limiteAvion) {
                int excesoDespues = ocupacionDespues - limiteAvion;
                int excesoAntes = Math.max(0, (ocupacionDespues - cantidad) - limiteAvion);
                deltaPenalidad += ((excesoDespues - excesoAntes) * 5000.0); // Sumamos la nueva multa
            }
        }

        return deltaPenalidad;
    }

    /**
     * Versión modificada del BFS que ignora un vuelo prohibido para forzar al sistema
     * a descubrir escalas y rutas que el algoritmo Voraz ignoró.
     */
    private List<Vuelo> buscarRutaAlternativaBFS(String origen, String destino, Map<String, List<Vuelo>> vuelosPorOrigen, Vuelo vueloProhibido) {
        Queue<List<Vuelo>> cola = new LinkedList<>();
        Set<String> visitados = new HashSet<>();

        if (vuelosPorOrigen.containsKey(origen)) {
            for (Vuelo v : vuelosPorOrigen.get(origen)) {
                // AQUÍ ESTÁ LA MAGIA: Ignoramos el vuelo prohibido
                if (v.equals(vueloProhibido)) continue;

                List<Vuelo> rutaInicial = new ArrayList<>();
                rutaInicial.add(v);
                cola.add(rutaInicial);
            }
            visitados.add(origen);
        }

        while (!cola.isEmpty()) {
            List<Vuelo> rutaActual = cola.poll();
            Vuelo ultimoVuelo = rutaActual.get(rutaActual.size() - 1);
            String aeropuertoActual = ultimoVuelo.getDestino();

            if (aeropuertoActual.equals(destino)) return rutaActual;

            if (!visitados.contains(aeropuertoActual) && vuelosPorOrigen.containsKey(aeropuertoActual)) {
                visitados.add(aeropuertoActual);
                for (Vuelo conexion : vuelosPorOrigen.get(aeropuertoActual)) {
                    // --- EL FILTRO MÁGICO: Lógica de Tiempo ---
                    if (TimeCalculator.esConexionFisicamentePosible(ultimoVuelo, conexion)) {
                        long espera = TimeCalculator.calcularTiempoEsperaMinutos(ultimoVuelo, conexion);
                        if (espera <= 18 * 60) {
                            List<Vuelo> nuevaRuta = new ArrayList<>(rutaActual);
                            nuevaRuta.add(conexion);
                            cola.add(nuevaRuta);
                        }
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    private String obtenerAtributoMovimiento(Solucion anterior, Solucion nueva) {
        // En este diseño simplificado, el atributo tabú será el ID del pedido que movimos.
        // Si movimos el pedido 005, el ID "005" entra a la Lista Tabú y no podremos
        // volver a cambiar su ruta durante las siguientes 15 iteraciones.
        for (Map.Entry<String, List<Vuelo>> entry : nueva.getRutasAsignadas().entrySet()) {
            String idPedido = entry.getKey();
            List<Vuelo> rutaNueva = entry.getValue();
            List<Vuelo> rutaVieja = anterior.getRutasAsignadas().get(idPedido);

            if (!rutaNueva.equals(rutaVieja)) {
                return idPedido; // Retornamos el culpable del cambio
            }
        }
        return "DESCONOCIDO";
    }

    private void actualizarListaTabu(Map<String, Integer> listaTabu) {
        listaTabu.replaceAll((k, v) -> v - 1);
        listaTabu.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    private void registrarImpactoAeropuertos(Solucion solucion, Pedido pedido, List<Vuelo> ruta, int factor, Map<String, Aeropuerto> mapaAeros) {
        if (ruta == null || ruta.isEmpty()) return;

        java.time.LocalDateTime tiempoActual = pedido.getFechaRegistro();
        int cantidad = pedido.getCantidadMaletas() * factor;

        // 1. IMPACTO EN EL ORIGEN INICIAL (Solo se cuenta una vez al registrar el pedido)
        Vuelo vInicial = ruta.get(0);
        String horaOrigen = tiempoActual.toLocalDate().toString() + "_" + tiempoActual.getHour();
        String keyOrigen = vInicial.getOrigen() + "_" + horaOrigen;
        int actualOrigen = solucion.getOcupacionAeropuertos().getOrDefault(keyOrigen, 0);
        solucion.getOcupacionAeropuertos().put(keyOrigen, Math.max(0, actualOrigen + cantidad));

        for (int i = 0; i < ruta.size(); i++) {
            Vuelo v = ruta.get(i);

            // IMPACTO EN VUELO
            String diaVuelo = tiempoActual.toLocalDate().toString();
            String idVueloUnico = v.getOrigen() + "-" + v.getDestino() + "-" + v.getHoraSalida() + "_" + diaVuelo;
            int maletasVuelo = solucion.getOcupacionVuelos().getOrDefault(idVueloUnico, 0);
            solucion.getOcupacionVuelos().put(idVueloUnico, Math.max(0, maletasVuelo + cantidad));

            // RELOJ: Adelantamos tiempo del vuelo usando TimeCalculator
            Aeropuerto origen = mapaAeros.get(v.getOrigen());
            Aeropuerto destino = mapaAeros.get(v.getDestino());
            if(origen != null && destino != null){
                long duracionReal = TimeCalculator.calcularDuracionVueloMinutos(v, origen, destino);
                tiempoActual = tiempoActual.plusMinutes(duracionReal);
            }

            // 2. IMPACTO EN DESTINO (Se cuenta al aterrizar en esa hora específica)
            String horaDestino = tiempoActual.toLocalDate().toString() + "_" + tiempoActual.getHour();
            String keyDestino = v.getDestino() + "_" + horaDestino;
            int actualDestino = solucion.getOcupacionAeropuertos().getOrDefault(keyDestino, 0);
            solucion.getOcupacionAeropuertos().put(keyDestino, Math.max(0, actualDestino + cantidad));

            // RELOJ: Adelantamos tiempo de espera en escala
            if (i < ruta.size() - 1) {
                Vuelo next = ruta.get(i+1);
                long espera = TimeCalculator.calcularTiempoEsperaMinutos(v, next);
                tiempoActual = tiempoActual.plusMinutes(espera);
            }
        }
    }

    private double calcularCostoRutaUnica(Pedido pedido, List<Vuelo> ruta, Map<String, Aeropuerto> mapaAeros) {
        if (ruta == null || ruta.isEmpty()) {
            return 999999.0; // Penalización por pedido sin ruta
        }

        double costoRuta = 0.0;
        long tiempoTotalRutaMinutos = 0;

        Aeropuerto origenPedido = mapaAeros.get(pedido.getOrigen());
        Aeropuerto destinoPedido = mapaAeros.get(pedido.getDestino());

        // Validar destino final de la ruta
        Vuelo ultimoVuelo = ruta.get(ruta.size() - 1);
        if (!ultimoVuelo.getDestino().equals(pedido.getDestino())) {
            return 999999.0;
        }

        for (int i = 0; i < ruta.size(); i++) {
            Vuelo vueloActual = ruta.get(i);
            Aeropuerto origenVuelo = mapaAeros.get(vueloActual.getOrigen());
            Aeropuerto destinoVuelo = mapaAeros.get(vueloActual.getDestino());

            // A. Sumar tiempo de vuelo
            tiempoTotalRutaMinutos += TimeCalculator.calcularDuracionVueloMinutos(vueloActual, origenVuelo, destinoVuelo);

            // B. Sumar tiempo de espera y validar conexión (mínimo 10 min)
            if (i < ruta.size() - 1) {
                Vuelo vueloSiguiente = ruta.get(i + 1);
                if (!TimeCalculator.esConexionFisicamentePosible(vueloActual, vueloSiguiente)) {
                    return 999999.0;
                }
                tiempoTotalRutaMinutos += TimeCalculator.calcularTiempoEsperaMinutos(vueloActual, vueloSiguiente);
            }
        }

        // Regla de negocio: Tiempo de recojo final (10 min)
        tiempoTotalRutaMinutos += TimeCalculator.TIEMPO_RECOJO_FINAL;

        // El costo base es el tiempo en minutos (para que el algoritmo prefiera lo más rápido)
        costoRuta += tiempoTotalRutaMinutos;

        // C. Penalización por SLA
        boolean esMismoContinente = origenPedido.getContinente().equals(destinoPedido.getContinente());
        costoRuta += TimeCalculator.calcularPenalizacionTiempo(tiempoTotalRutaMinutos, esMismoContinente);

        return costoRuta;
    }

    public void realizarDiagnosticoColapso(Solucion sol, List<Pedido> pedidos, Map<String, Aeropuerto> mapaAeros, Map<String, Vuelo> mapaVuelos) {
        System.out.println("\n========== DIAGNÓSTICO DE COLAPSO LOGÍSTICO ==========");
        int fallosSLA = 0;
        int fallosVuelo = 0;
        int fallosAero = 0;

        // 1. Auditoría de SLA (Tiempos de entrega)
        for (Pedido p : pedidos) {
            List<Vuelo> ruta = sol.getRutasAsignadas().get(p.getIdPedido());
            if (ruta == null || ruta.isEmpty()) continue;

            // Calculamos el tiempo total real en minutos
            long tiempoTotalMinutos = calcularSoloTiempoEnMinutos(p, ruta, mapaAeros);

            Aeropuerto origen = mapaAeros.get(p.getOrigen());
            Aeropuerto destino = mapaAeros.get(p.getDestino());
            boolean esMismoContinente = origen.getContinente().equals(destino.getContinente());

            // Definimos el límite legal según el caso
            long limiteLegal = esMismoContinente ? 1440 : 2880; // 24h o 48h

            if (tiempoTotalMinutos > limiteLegal) {
                System.out.printf("[FALLO SLA] Pedido %s: %d min (Límite: %d min). Retraso de %d min.%n",
                        p.getIdPedido(), tiempoTotalMinutos, limiteLegal, (tiempoTotalMinutos - limiteLegal));
                fallosSLA++;
            }
        }

        // 2. Auditoría de Vuelos (Capacidad de aviones)
        for (Map.Entry<String, Integer> entry : sol.getOcupacionVuelos().entrySet()) {
            String idVueloFull = entry.getKey(); // Ej: "LIM-MAD-14:30_2026-01-02"
            int ocupacion = entry.getValue();

            String idVueloBase = idVueloFull.split("_")[0];
            Vuelo v = mapaVuelos.get(idVueloBase);

            if (v != null && ocupacion > v.getCapacidadMax()) {
                System.out.printf("[FALLO VUELO] %s el %s: Ocupación %d > Límite %d maletas.%n",
                        idVueloBase, idVueloFull.split("_")[1], ocupacion, v.getCapacidadMax());
                fallosVuelo++;
            }
        }

        // 3. Auditoría de Aeropuertos (Almacenes)
        for (Map.Entry<String, Integer> entry : sol.getOcupacionAeropuertos().entrySet()) {
            String keyAero = entry.getKey(); // Ej: "LIM_2026-01-02_14"
            int ocupacion = entry.getValue();

            String codAero = keyAero.split("_")[0];
            Aeropuerto a = mapaAeros.get(codAero);

            if (a != null && ocupacion > a.getCapacidadMax()) {
                System.out.printf("[FALLO ALMACÉN] %s el %s (hora %s): Ocupación %d > Límite %d.%n",
                        codAero, keyAero.split("_")[1], keyAero.split("_")[2], ocupacion, a.getCapacidadMax());
                fallosAero++;
            }
        }

        System.out.println("------------------------------------------------------");
        System.out.println("RESUMEN: SLA: " + fallosSLA + " | Vuelos: " + fallosVuelo + " | Almacenes: " + fallosAero);
        System.out.println("======================================================\n");
    }

    private long calcularSoloTiempoEnMinutos(Pedido p, List<Vuelo> ruta, Map<String, Aeropuerto> mapaAeros) {
        long minutos = 0;
        for (int i = 0; i < ruta.size(); i++) {
            Vuelo v = ruta.get(i);
            minutos += TimeCalculator.calcularDuracionVueloMinutos(v, mapaAeros.get(v.getOrigen()), mapaAeros.get(v.getDestino()));
            if (i < ruta.size() - 1) {
                minutos += TimeCalculator.calcularTiempoEsperaMinutos(v, ruta.get(i + 1));
            }
        }
        return minutos + TimeCalculator.TIEMPO_RECOJO_FINAL;
    }
}