package com.grupo5e.morapack.tabusearch.algoritmo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Lista tabu FIFO con capacidad fija.
 *
 * Usa una cola doble para mantener el orden de insercion y un HashSet para
 * consultas O(1) "contiene". Cuando la cola excede la capacidad, se desaloja
 * el movimiento mas antiguo.
 *
 * Criterio de aspiracion: el TabuSearch puede aplicar un movimiento tabu si
 * este conduce a una solucion mejor que la mejor encontrada hasta el
 * momento. Ese chequeo lo hace el solver, esta clase solo responde "esta
 * prohibido ahora?".
 */
public class ListaTabu {

    private final int capacidad;
    private final Deque<String> orden = new ArrayDeque<>();
    private final Set<String> contenido = new HashSet<>();

    public ListaTabu(int capacidad) {
        this.capacidad = Math.max(1, capacidad);
    }

    public boolean contiene(String firma) {
        return contenido.contains(firma);
    }

    public void agregar(String firma) {
        if (contenido.contains(firma)) return;
        orden.addLast(firma);
        contenido.add(firma);
        while (orden.size() > capacidad) {
            String viejo = orden.pollFirst();
            if (viejo != null) contenido.remove(viejo);
        }
    }

    public int tamano() { return orden.size(); }
}
