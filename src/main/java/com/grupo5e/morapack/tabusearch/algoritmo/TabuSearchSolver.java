package com.grupo5e.morapack.tabusearch.algoritmo;

import com.grupo5e.morapack.tabusearch.config.ConfiguracionTabu;
import com.grupo5e.morapack.tabusearch.evaluacion.EvaluadorSolucion;
import com.grupo5e.morapack.tabusearch.modelo.AsignacionEnvio;
import com.grupo5e.morapack.tabusearch.modelo.Envio;
import com.grupo5e.morapack.tabusearch.modelo.Ruta;
import com.grupo5e.morapack.tabusearch.modelo.Solucion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Implementacion del metaheuristico Tabu Search para la planificacion de
 * rutas de envios del caso TASF-B2B.
 *
 * Esquema general:
 *
 *   solucion_actual  <- solucion_inicial
 *   mejor_solucion   <- solucion_actual
 *   lista_tabu       <- vacia
 *
 *   para cada iteracion (hasta maxIteracionesTabu o tiempoMaxTabuMs):
 *      vecindario = generar movimientos candidatos (cambiar ruta de algunos envios)
 *      mejor_movimiento = argmin_{m in vecindario} coste(aplicar(solucion_actual, m))
 *                         sujeto a: m no tabu  OR  aplicar(m) mejora a mejor_solucion (aspiracion)
 *      si no hay movimiento admisible, se termina.
 *      aplicar(mejor_movimiento) a solucion_actual
 *      agregar firma(mejor_movimiento) a lista_tabu
 *      si coste(solucion_actual) < coste(mejor_solucion): mejor_solucion = solucion_actual
 *
 * Movimiento:
 *   "cambiar la ruta del envio X por otra de sus K rutas candidatas"
 *
 * Vecindario:
 *   Por rendimiento, no generamos TODOS los movimientos. En cada iteracion se
 *   elige una sub-muestra de envios (prioridad a los que estan tarde o sin
 *   ruta) y para cada uno se proponen hasta K rutas alternativas.
 *
 * Evaluacion incremental:
 *   Aplicar un movimiento significa: retirar capacidades de la ruta vieja,
 *   sumar capacidades de la ruta nueva, recomputar valor objetivo. Se valida
 *   que la nueva ruta quepa en capacidades. Si no, se descarta la alternativa.
 */
public class TabuSearchSolver {

    private final ConfiguracionTabu cfg;
    private final BuscadorRutas buscador;
    private final EstadoCapacidades capacidades;
    private final EvaluadorSolucion evaluador;
    private final Random rng;

    public TabuSearchSolver(ConfiguracionTabu cfg,
                            BuscadorRutas buscador,
                            EstadoCapacidades capacidades,
                            EvaluadorSolucion evaluador) {
        this.cfg = cfg;
        this.buscador = buscador;
        this.capacidades = capacidades;
        this.evaluador = evaluador;
        this.rng = new Random(cfg.semilla);
    }

    /**
     * Ejecuta el TabuSearch sobre una solucion inicial y devuelve la mejor
     * solucion encontrada.
     */
    public Solucion optimizar(Solucion inicial) {

        Solucion actual = inicial;
        Solucion mejor = inicial.copiaSuperficial();
        ListaTabu tabu = new ListaTabu(cfg.tamanoListaTabu);

        long tInicio = System.currentTimeMillis();

        System.out.println();
        System.out.println("[TABU] ================ TABU SEARCH =================");
        System.out.println("[TABU] Iter 0: objetivo=" + mejor.getValorObjetivo()
                + " tarde=" + mejor.getEnviosTarde()
                + " sinRuta=" + mejor.getEnviosSinRuta()
                + " aTiempo=" + mejor.getEnviosEntregadosATiempo());

        int iteracionesSinMejora = 0;
        int maxSinMejora = Math.max(10, cfg.maxIteracionesTabu / 2);

        for (int iter = 1; iter <= cfg.maxIteracionesTabu; iter++) {
            long tAhora = System.currentTimeMillis();
            if (tAhora - tInicio > cfg.tiempoMaxTabuMs) {
                System.out.println("[TABU] Tiempo maximo alcanzado en iter " + iter);
                break;
            }

            Movimiento mejorMov = explorarVecindario(actual, tabu, mejor);

            if (mejorMov == null) {
                System.out.println("[TABU] Iter " + iter + ": no hay movimientos admisibles, parando.");
                break;
            }

            aplicarMovimiento(actual, mejorMov);
            tabu.agregar(mejorMov.firmaReversa()); // prohibido volver al estado anterior
            evaluador.evaluar(actual, capacidades);

            if (actual.getValorObjetivo() < mejor.getValorObjetivo()) {
                mejor = actual.copiaSuperficial();
                iteracionesSinMejora = 0;
                System.out.println("[TABU] Iter " + iter + " MEJORA -> obj="
                        + mejor.getValorObjetivo()
                        + " tarde=" + mejor.getEnviosTarde()
                        + " sinRuta=" + mejor.getEnviosSinRuta()
                        + " aTiempo=" + mejor.getEnviosEntregadosATiempo());
            } else {
                iteracionesSinMejora++;
                if (iter % 5 == 0) {
                    System.out.println("[TABU] Iter " + iter + " obj(actual)="
                            + actual.getValorObjetivo() + "  mejor=" + mejor.getValorObjetivo());
                }
            }

            if (iteracionesSinMejora >= maxSinMejora) {
                System.out.println("[TABU] " + maxSinMejora + " iters sin mejora, parando.");
                break;
            }
        }

        System.out.println("[TABU] FIN  mejor objetivo = " + mejor.getValorObjetivo()
                + "  tarde=" + mejor.getEnviosTarde()
                + "  sinRuta=" + mejor.getEnviosSinRuta()
                + "  aTiempo=" + mejor.getEnviosEntregadosATiempo());

        // Restaurar el estado de capacidades y la solucion al "mejor" antes de devolver.
        sincronizarConMejor(actual, mejor);

        return mejor;
    }

    /**
     * Explora un subconjunto de movimientos: por cada envio candidato busca
     * hasta K rutas alternativas y calcula el delta de aplicar cada una.
     * Devuelve el mejor movimiento admisible.
     */
    private Movimiento explorarVecindario(Solucion actual, ListaTabu tabu, Solucion mejor) {

        // Seleccionar envios candidatos: priorizar los que estan tarde o sin ruta
        List<AsignacionEnvio> tarde = new ArrayList<>();
        List<AsignacionEnvio> aTiempo = new ArrayList<>();
        for (AsignacionEnvio a : actual.lista()) {
            if (a.getRuta() == null || a.getRuta().esVacia() || !a.isLlegaATiempo()) tarde.add(a);
            else aTiempo.add(a);
        }

        // Tomamos todos los tarde + una fraccion aleatoria de los a-tiempo
        Collections.shuffle(aTiempo, rng);
        int limiteATiempo = Math.min(aTiempo.size(), Math.max(5, tarde.size()));
        List<AsignacionEnvio> candidatos = new ArrayList<>(tarde);
        candidatos.addAll(aTiempo.subList(0, limiteATiempo));
        Collections.shuffle(candidatos, rng);

        Movimiento mejorMov = null;

        for (AsignacionEnvio a : candidatos) {
            Envio e = a.getEnvio();
            Ruta rVieja = a.getRuta();

            // Desinstalar temporalmente las capacidades de la ruta vieja para
            // evaluar rutas nuevas "en vacio" respecto a este envio.
            if (rVieja != null && !rVieja.esVacia()) {
                capacidades.aplicar(rVieja, e.getCantidad(), -1);
            }

            // Generar rutas candidatas exigiendo capacidad vuelo+aeropuerto
            List<Ruta> candidatasRuta = buscador.buscar(e, capacidades, 2, cfg.candidatasPorEnvio);
            if (candidatasRuta.isEmpty()) {
                candidatasRuta = buscador.buscar(e, capacidades, 1, cfg.candidatasPorEnvio);
            }

            // Restaurar la ruta vieja en capacidades (aun no hemos movido nada)
            if (rVieja != null && !rVieja.esVacia()) {
                capacidades.aplicar(rVieja, e.getCantidad(), +1);
            }

            for (Ruta rNueva : candidatasRuta) {
                // Saltar si es la misma ruta (no tiene sentido)
                if (rVieja != null && rNueva.firma().equals(rVieja.firma())) continue;

                Movimiento m = new Movimiento(e.getId(), rVieja, rNueva,
                        estimarDelta(e, rVieja, rNueva));

                boolean esTabu = tabu.contiene(m.firma());

                // Aspiracion: si el delta lleva a una solucion mejor que la mejor global, permitir.
                if (esTabu) {
                    double nuevoObj = actual.getValorObjetivo() + m.deltaValor;
                    if (nuevoObj >= mejor.getValorObjetivo()) continue;
                }

                if (mejorMov == null || m.deltaValor < mejorMov.deltaValor) {
                    mejorMov = m;
                }
            }
        }

        return mejorMov;
    }

    /**
     * Estima la variacion del valor objetivo al cambiar de rVieja a rNueva
     * SOLO para este envio (los demas envios no cambian). Se basa en el
     * criterio tiempoEnRed y si cumple plazo.
     */
    private double estimarDelta(Envio e, Ruta rVieja, Ruta rNueva) {
        double costoVieja = costoEnvio(e, rVieja);
        double costoNueva = costoEnvio(e, rNueva);
        return costoNueva - costoVieja;
    }

    private double costoEnvio(Envio e, Ruta r) {
        if (r == null || r.esVacia()) {
            return cfg.pesoSinRuta;
        }
        int tiempo = (r.minutoLlegada() + cfg.tiempoLiberacionDestinoMin)
                - e.getInstanteGeneracionMinGmt();
        if (tiempo < 0) tiempo = 0;
        int plazo = evaluador.calcularPlazo(e);
        double c = cfg.pesoMinutoEnRed * tiempo;
        if (tiempo > plazo) {
            c += cfg.pesoEnvioTarde;
            c += cfg.pesoMinutoRetraso * (tiempo - plazo);
        }
        return c;
    }

    /**
     * Aplica un movimiento: retira capacidades de la ruta vieja del envio y
     * agrega las de la ruta nueva, luego cambia la ruta en la asignacion.
     */
    private void aplicarMovimiento(Solucion s, Movimiento m) {
        AsignacionEnvio a = s.obtener(m.idEnvio);
        if (a == null) return;
        int cantidad = a.getEnvio().getCantidad();
        capacidades.aplicar(a.getRuta(), cantidad, -1);
        a.setRuta(m.rutaNueva);
        capacidades.aplicar(m.rutaNueva, cantidad, +1);
    }

    /**
     * Reajusta el estado de capacidades a partir de la mejor solucion para
     * que un consumidor externo (reporte) vea una vista consistente.
     */
    private void sincronizarConMejor(Solucion actual, Solucion mejor) {
        // Retira todo lo que estuviera en "actual"
        for (AsignacionEnvio a : actual.lista()) {
            capacidades.aplicar(a.getRuta(), a.getEnvio().getCantidad(), -1);
        }
        // Aplica la mejor
        for (AsignacionEnvio a : mejor.lista()) {
            capacidades.aplicar(a.getRuta(), a.getEnvio().getCantidad(), +1);
        }
    }
}
