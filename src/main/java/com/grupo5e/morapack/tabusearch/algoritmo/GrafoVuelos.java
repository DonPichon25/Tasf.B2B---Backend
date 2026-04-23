package com.grupo5e.morapack.tabusearch.algoritmo;

import com.grupo5e.morapack.tabusearch.modelo.Vuelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Indice auxiliar de vuelos agrupados por aeropuerto origen, ordenados por
 * hora de salida GMT ascendente.
 *
 * Acelera las busquedas de rutas: al expandir un nodo (aeropuerto, tiempo),
 * obtenemos directamente la lista de vuelos candidatos desde ese aeropuerto,
 * en vez de recorrer todo el vector de vuelos en cada paso.
 */
public class GrafoVuelos {

    private final List<Vuelo> todos;
    private final Map<String, List<Vuelo>> porOrigen = new HashMap<>();

    public GrafoVuelos(List<Vuelo> vuelos) {
        this.todos = vuelos;
        for (Vuelo v : vuelos) {
            porOrigen.computeIfAbsent(v.getOrigen(), k -> new ArrayList<>()).add(v);
        }
        for (List<Vuelo> l : porOrigen.values()) {
            l.sort(Comparator.comparingInt(Vuelo::getSalidaMinGmt));
        }
    }

    /** Devuelve todos los vuelos que salen desde un aeropuerto dado. */
    public List<Vuelo> salidasDesde(String codigo) {
        return porOrigen.getOrDefault(codigo, Collections.emptyList());
    }

    public List<Vuelo> todos() {
        return todos;
    }

    /** Numero total de vuelos en el grafo. */
    public int tamano() {
        return todos.size();
    }
}
