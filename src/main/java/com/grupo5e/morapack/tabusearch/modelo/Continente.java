package com.grupo5e.morapack.tabusearch.modelo;

/**
 * Enumera los tres continentes con los que trabaja el caso TASF-B2B.
 *
 * Se usa para:
 *  - Clasificar cada {@link Aeropuerto}.
 *  - Decidir el plazo maximo de entrega de un envio:
 *      * mismo continente  -> plazoMismoContinente  (24h por defecto)
 *      * distinto continente -> plazoDistintoContinente (48h por defecto)
 *
 * Se mantiene como enum (en lugar de String) para evitar errores tipograficos
 * en comparaciones durante la planificacion.
 */
public enum Continente {
    AMERICA_DEL_SUR,
    EUROPA,
    ASIA;

    /**
     * Devuelve true si dos continentes son iguales.
     * Metodo auxiliar usado por el evaluador de rutas al calcular plazos.
     */
    public boolean mismoQue(Continente otro) {
        return otro != null && this == otro;
    }
}
