package com.grupo5e.morapack.tabusearch.algoritmo;

import com.grupo5e.morapack.tabusearch.config.ConfiguracionTabu;
import com.grupo5e.morapack.tabusearch.modelo.Envio;
import com.grupo5e.morapack.tabusearch.modelo.Ruta;
import com.grupo5e.morapack.tabusearch.modelo.Vuelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Genera rutas candidatas para un envio, desde su origen hasta su destino,
 * usando una variante de busqueda por mejor primero (best-first) sobre el
 * grafo de vuelos.
 *
 * Cada estado:
 *   (aeropuertoActual, minutoGmtActual, listaVuelos, visitados)
 *
 * Expansion: para cada vuelo saliendo de aeropuertoActual con
 *   salidaMinGmt >= minutoGmtActual + tiempoEscala,
 * se produce un estado sucesor. Se evita revisitar aeropuertos en la misma
 * ruta (no se vuelve a pasar por el mismo aeropuerto).
 *
 * Se limitan:
 *  - profundidad: maxTramosPorRuta
 *  - numero de candidatas: configuracion.candidatasPorEnvio
 *  - capacidad disponible del vuelo y del aeropuerto destino (opcional)
 *
 * La funcion objetivo del buscador es "llegar cuanto antes".
 */
public class BuscadorRutas {

    private final GrafoVuelos grafo;
    private final ConfiguracionTabu cfg;

    public BuscadorRutas(GrafoVuelos grafo, ConfiguracionTabu cfg) {
        this.grafo = grafo;
        this.cfg = cfg;
    }

    /**
     * Busca hasta {@code limite} rutas para {@code envio}.
     *
     * @param envio          envio a rutar.
     * @param capacidades    opcional; si no es null, filtra vuelos sin cupo.
     * @param capCheckLevel  si > 0, filtra agresivamente por capacidad.
     *                       0 = ignora capacidad.
     *                       1 = exige cupo en vuelo.
     *                       2 = exige cupo en vuelo y en almacen destino.
     * @param limite         maximo de rutas a devolver. Usa cfg.candidatasPorEnvio si es <= 0.
     */
    public List<Ruta> buscar(Envio envio, EstadoCapacidades capacidades,
                             int capCheckLevel, int limite) {

        int maxRutas = limite > 0 ? limite : cfg.candidatasPorEnvio;
        int maxProf = cfg.maxTramosPorRuta;
        int escala = cfg.tiempoEscalaMin;

        List<Ruta> resultado = new ArrayList<>();

        // Cola prioritaria por minuto de llegada actual + profundidad (heuristica)
        PriorityQueue<Nodo> cola = new PriorityQueue<>(
                Comparator.<Nodo>comparingInt(n -> n.minutoActualGmt)
                        .thenComparingInt(n -> n.tramos.size()));

        Nodo inicio = new Nodo(envio.getOrigen(), envio.getInstanteGeneracionMinGmt(),
                new ArrayList<>(), new HashSet<>());
        inicio.visitados.add(envio.getOrigen());
        cola.add(inicio);

        // Limite total de expansiones para que el buscador no se dispare.
        int expansionesMax = 5_000;
        int expansiones = 0;

        // Firmas ya incluidas en resultado (evita duplicados exactos).
        Set<String> firmasIncluidas = new HashSet<>();

        while (!cola.isEmpty() && resultado.size() < maxRutas && expansiones < expansionesMax) {
            Nodo n = cola.poll();
            expansiones++;

            if (n.aeropuerto.equals(envio.getDestino()) && !n.tramos.isEmpty()) {
                Ruta r = new Ruta(new ArrayList<>(n.tramos));
                if (firmasIncluidas.add(r.firma())) {
                    resultado.add(r);
                }
                continue;
            }
            if (n.tramos.size() >= maxProf) continue;

            List<Vuelo> salidas = grafo.salidasDesde(n.aeropuerto);
            for (Vuelo v : salidas) {
                // Compatibilidad temporal: vuelo debe salir despues de minutoActual + escala
                int minSalidaRequerida = n.tramos.isEmpty()
                        ? n.minutoActualGmt                  // primera salida: inmediato desde instanteGeneracion
                        : n.minutoActualGmt + escala;        // siguientes: tras escala

                if (v.getSalidaMinGmt() < minSalidaRequerida) continue;
                if (n.visitados.contains(v.getDestino())) continue;

                // Filtros de capacidad
                if (capacidades != null && capCheckLevel >= 1) {
                    if (capacidades.capacidadDisponibleVuelo(v) < envio.getCantidad()) continue;
                }
                if (capacidades != null && capCheckLevel >= 2) {
                    if (capacidades.capacidadDisponibleAeropuerto(v.getDestino()) < envio.getCantidad())
                        continue;
                }

                List<Vuelo> nuevosTramos = new ArrayList<>(n.tramos);
                nuevosTramos.add(v);
                Set<String> nuevosVis = new HashSet<>(n.visitados);
                nuevosVis.add(v.getDestino());

                Nodo hijo = new Nodo(v.getDestino(), v.getLlegadaMinGmt(), nuevosTramos, nuevosVis);
                cola.add(hijo);
            }
        }

        // Ordenar por hora de llegada ascendente
        resultado.sort(Comparator.comparingInt(Ruta::minutoLlegada));
        return resultado;
    }

    /** Nodo auxiliar para la busqueda. */
    private static class Nodo {
        final String aeropuerto;
        final int minutoActualGmt;
        final List<Vuelo> tramos;
        final Set<String> visitados;

        Nodo(String aeropuerto, int minutoActualGmt,
             List<Vuelo> tramos, Set<String> visitados) {
            this.aeropuerto = aeropuerto;
            this.minutoActualGmt = minutoActualGmt;
            this.tramos = tramos;
            this.visitados = visitados;
        }
    }
}
