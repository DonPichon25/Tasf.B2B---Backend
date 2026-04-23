package com.grupo5e.morapack.tabusearch.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Una Ruta es una secuencia ordenada de vuelos que resuelve el traslado de un
 * envio desde su aeropuerto origen hasta su aeropuerto destino.
 *
 * Una ruta valida cumple:
 *  - El origen del primer vuelo coincide con el origen del envio.
 *  - El destino del ultimo vuelo coincide con el destino del envio.
 *  - Para cada par de vuelos consecutivos, el destino del anterior es el
 *    origen del siguiente.
 *  - La salida del siguiente >= llegada del anterior + tiempoEscala (10 min).
 *
 * Esta clase NO valida, solo agrupa. La validacion la hace el evaluador.
 *
 * Se usa como "movimiento" clave en TabuSearch: cambiar la ruta de un envio
 * equivale a cambiar este objeto por otro.
 */
public final class Ruta {

    private final List<Vuelo> vuelos;

    public Ruta(List<Vuelo> vuelos) {
        this.vuelos = List.copyOf(vuelos);
    }

    public static Ruta vacia() {
        return new Ruta(Collections.emptyList());
    }

    public List<Vuelo> getVuelos() { return vuelos; }

    public int cantidadTramos() { return vuelos.size(); }

    public boolean esVacia() { return vuelos.isEmpty(); }

    /** Minuto GMT en que sale el primer vuelo de la ruta. */
    public int minutoSalida() {
        if (vuelos.isEmpty()) return -1;
        return vuelos.get(0).getSalidaMinGmt();
    }

    /** Minuto GMT en que llega el ultimo vuelo al destino. */
    public int minutoLlegada() {
        if (vuelos.isEmpty()) return -1;
        return vuelos.get(vuelos.size() - 1).getLlegadaMinGmt();
    }

    /**
     * Firma textual estable para la lista tabu.
     * Dos rutas con la misma secuencia de vuelos comparten firma.
     */
    public String firma() {
        StringBuilder sb = new StringBuilder();
        for (Vuelo v : vuelos) {
            sb.append(v.getId()).append('|');
        }
        return sb.toString();
    }

    /** Devuelve una copia mutable de los vuelos (util para construir vecinos). */
    public List<Vuelo> copiaMutable() {
        return new ArrayList<>(vuelos);
    }

    @Override
    public String toString() {
        if (vuelos.isEmpty()) return "(ruta vacia)";
        StringBuilder sb = new StringBuilder();
        sb.append(vuelos.get(0).getOrigen());
        for (Vuelo v : vuelos) {
            sb.append(" -[").append(v.getHoraSalidaLocalTxt()).append("->")
                    .append(v.getHoraLlegadaLocalTxt()).append("]-> ")
                    .append(v.getDestino());
        }
        return sb.toString();
    }
}
