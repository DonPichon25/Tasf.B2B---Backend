package com.grupo5e.morapack.tabusearch.algoritmo;

import com.grupo5e.morapack.tabusearch.config.ConfiguracionTabu;
import com.grupo5e.morapack.tabusearch.evaluacion.EvaluadorSolucion;
import com.grupo5e.morapack.tabusearch.modelo.AsignacionEnvio;
import com.grupo5e.morapack.tabusearch.modelo.Envio;
import com.grupo5e.morapack.tabusearch.modelo.Ruta;
import com.grupo5e.morapack.tabusearch.modelo.Solucion;

import java.util.Comparator;
import java.util.List;

/**
 * Genera una solucion inicial factible ordenando los envios por "criticidad"
 * y asignando a cada uno la mejor ruta disponible considerando capacidades
 * ya usadas por envios previos.
 *
 * Criterio de criticidad: plazo menos tiempo actual (los que menos margen
 * tienen primero). Los envios inter-continente se toman despues porque su
 * plazo es mas holgado en muchos casos.
 *
 * Para cada envio:
 *   1. Se pide al {@link BuscadorRutas} un conjunto de rutas candidatas con
 *      filtro de capacidad vuelo+aeropuerto.
 *   2. Se escoge la primera (la de llegada mas temprana).
 *   3. Si no hay candidatas, se intenta una segunda vez relajando el filtro
 *      de capacidad de aeropuerto (solo vuelo).
 *   4. Si sigue sin haber, se deja el envio sin ruta (penalizacion alta).
 */
public class ConstructorInicial {

    private final BuscadorRutas buscador;
    private final EstadoCapacidades capacidades;
    private final EvaluadorSolucion evaluador;
    private final ConfiguracionTabu cfg;

    public ConstructorInicial(BuscadorRutas buscador,
                              EstadoCapacidades capacidades,
                              EvaluadorSolucion evaluador,
                              ConfiguracionTabu cfg) {
        this.buscador = buscador;
        this.capacidades = capacidades;
        this.evaluador = evaluador;
        this.cfg = cfg;
    }

    public Solucion construir(List<Envio> envios) {
        Solucion s = new Solucion();

        // Orden por criticidad ligera: mas tempranos primero
        envios.sort(Comparator.comparingInt(Envio::getInstanteGeneracionMinGmt));

        int asignados = 0;
        int sinRuta = 0;

        for (Envio e : envios) {
            Ruta elegida = seleccionarRuta(e);

            AsignacionEnvio a = new AsignacionEnvio(e, elegida == null ? Ruta.vacia() : elegida);
            if (elegida != null) {
                capacidades.aplicar(elegida, e.getCantidad(), +1);
                asignados++;
            } else {
                sinRuta++;
            }
            s.agregar(a);
        }

        evaluador.evaluar(s, capacidades);

        System.out.println("[INIT] Envios totales         : " + envios.size());
        System.out.println("[INIT] Asignados con ruta     : " + asignados);
        System.out.println("[INIT] Sin ruta inicialmente  : " + sinRuta);
        System.out.println("[INIT] Entregados a tiempo    : " + s.getEnviosEntregadosATiempo());
        System.out.println("[INIT] Entregados tarde       : " + s.getEnviosTarde());
        System.out.println("[INIT] Valor objetivo inicial : " + s.getValorObjetivo());

        return s;
    }

    /**
     * Intenta encontrar una ruta con capacidad completa; si no hay, relaja
     * restricciones progresivamente (solo vuelo, y finalmente sin capacidad).
     */
    private Ruta seleccionarRuta(Envio e) {
        // Nivel 2: vuelo + aeropuerto
        List<Ruta> rs = buscador.buscar(e, capacidades, 2, cfg.candidatasPorEnvio);
        if (!rs.isEmpty()) return rs.get(0);

        // Nivel 1: solo vuelo
        rs = buscador.buscar(e, capacidades, 1, cfg.candidatasPorEnvio);
        if (!rs.isEmpty()) return rs.get(0);

        // Nivel 0: sin filtro de capacidad (ruta viable temporal, se reprogramara con Tabu)
        rs = buscador.buscar(e, null, 0, cfg.candidatasPorEnvio);
        if (!rs.isEmpty()) return rs.get(0);

        return null;
    }
}
