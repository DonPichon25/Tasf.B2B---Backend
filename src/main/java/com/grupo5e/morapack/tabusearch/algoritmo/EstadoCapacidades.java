package com.grupo5e.morapack.tabusearch.algoritmo;

import com.grupo5e.morapack.tabusearch.modelo.Aeropuerto;
import com.grupo5e.morapack.tabusearch.modelo.Ruta;
import com.grupo5e.morapack.tabusearch.modelo.Vuelo;

import java.util.HashMap;
import java.util.Map;

/**
 * Mantiene la ocupacion actual de:
 *  - cada vuelo (total de maletas asignadas al vuelo)
 *  - cada aeropuerto (aproximacion instantanea: suma de maletas actualmente
 *    reservadas en ese aeropuerto por todos los envios asignados).
 *
 * Se utiliza como "libro de cuentas" que el TabuSearch actualiza cada vez que
 * asigna o retira una ruta.
 *
 * IMPORTANTE (simplificacion del modelo):
 *  - La ocupacion de aeropuerto es instantanea, no por ventana temporal.
 *    Esto es una aproximacion conservadora del almacen. En el caso real se
 *    deberia rastrear ocupacion por minuto; para el alcance del planificador
 *    con TabuSearch en una corrida de demostracion, basta con la version
 *    instantanea: alerta si una ruta hace que el destino exceda su capacidad.
 */
public class EstadoCapacidades {

    private final Map<Integer, Integer> ocupacionVuelo = new HashMap<>();
    private final Map<String, Integer> ocupacionAeropuerto = new HashMap<>();

    private final Map<String, Aeropuerto> aeropuertos;

    public EstadoCapacidades(Map<String, Aeropuerto> aeropuertos) {
        this.aeropuertos = aeropuertos;
    }

    /** Cantidad de maletas asignadas actualmente al vuelo v. */
    public int ocupacionVuelo(Vuelo v) {
        return ocupacionVuelo.getOrDefault(v.getId(), 0);
    }

    /** Cantidad disponible de asientos libres en el vuelo v. */
    public int capacidadDisponibleVuelo(Vuelo v) {
        return v.getCapacidadMaxima() - ocupacionVuelo(v);
    }

    /** Ocupacion instantanea del aeropuerto cod. */
    public int ocupacionAeropuerto(String cod) {
        return ocupacionAeropuerto.getOrDefault(cod, 0);
    }

    /** Cantidad disponible en el almacen del aeropuerto. */
    public int capacidadDisponibleAeropuerto(String cod) {
        Aeropuerto a = aeropuertos.get(cod);
        if (a == null) return 0;
        return a.getCapacidadMaxima() - ocupacionAeropuerto(cod);
    }

    /**
     * Registra (cantidad) maletas fluyendo por toda la ruta:
     *  - suma en cada vuelo su cantidad.
     *  - suma en cada aeropuerto intermedio / destino su cantidad (aproximacion).
     *
     * Si {@code signo} = +1 agrega. Si {@code signo} = -1 retira (rollback).
     */
    public void aplicar(Ruta r, int cantidad, int signo) {
        if (r == null || r.esVacia()) return;
        int delta = cantidad * signo;
        for (Vuelo v : r.getVuelos()) {
            ocupacionVuelo.merge(v.getId(), delta, Integer::sum);
            ocupacionAeropuerto.merge(v.getDestino(), delta, Integer::sum);
        }
    }

    /**
     * Verifica si agregar {@code cantidad} por la ruta romperia capacidades.
     */
    public boolean permiteAgregar(Ruta r, int cantidad) {
        if (r == null || r.esVacia()) return false;
        for (Vuelo v : r.getVuelos()) {
            if (capacidadDisponibleVuelo(v) < cantidad) return false;
            if (capacidadDisponibleAeropuerto(v.getDestino()) < cantidad) return false;
        }
        return true;
    }
}
