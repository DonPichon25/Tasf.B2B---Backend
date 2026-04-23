package com.grupo5e.morapack.tabusearch.algoritmo;

import com.grupo5e.morapack.tabusearch.modelo.Ruta;

/**
 * Representa un movimiento en el vecindario del TabuSearch.
 *
 * Un movimiento "cambia la ruta" de un envio dado por otra ruta alternativa.
 *
 * Campos:
 *  - idEnvio: identificador del envio afectado.
 *  - rutaAnterior: la ruta que el envio tenia antes del movimiento.
 *  - rutaNueva: la ruta propuesta.
 *  - deltaValor: variacion esperada del valor objetivo (nueva - anterior).
 *
 * Se usa como elemento en la lista tabu (ver {@link ListaTabu}): al aplicar
 * un movimiento, su firma (idEnvio + firma(rutaNueva)) queda "prohibida" por
 * un numero dado de iteraciones; mientras tanto, no se puede revisitar la
 * misma decision (salvo que se cumpla el criterio de aspiracion).
 */
public final class Movimiento {

    public final String idEnvio;
    public final Ruta rutaAnterior;
    public final Ruta rutaNueva;
    public final double deltaValor;

    public Movimiento(String idEnvio, Ruta rutaAnterior, Ruta rutaNueva, double deltaValor) {
        this.idEnvio = idEnvio;
        this.rutaAnterior = rutaAnterior;
        this.rutaNueva = rutaNueva;
        this.deltaValor = deltaValor;
    }

    /** Firma estable para la lista tabu. */
    public String firma() {
        return idEnvio + "#" + (rutaNueva == null ? "null" : rutaNueva.firma());
    }

    /** Firma reversa: identifica el movimiento de volver al estado anterior. */
    public String firmaReversa() {
        return idEnvio + "#" + (rutaAnterior == null ? "null" : rutaAnterior.firma());
    }
}
