package com.grupo5e.morapack.tabusearch.modelo;

/**
 * Par (envio, ruta) que indica como se esta planeando transportar un envio.
 *
 * Es mutable solo en el campo ruta: a lo largo del TabuSearch, el envio no
 * cambia pero la ruta si (cada "movimiento" reemplaza la ruta por otra).
 *
 * Se agrega un flag derivado {@code llegaATiempo} que se calcula con el
 * evaluador y se guarda aqui para evitar recalcularlo al imprimir el reporte.
 */
public final class AsignacionEnvio {

    private final Envio envio;
    private Ruta ruta;

    /** true si la ruta entrega dentro del plazo; se setea al evaluar. */
    private boolean llegaATiempo;

    /** Minutos totales desde el instante de envio hasta la llegada final. */
    private int tiempoEntregaMin;

    public AsignacionEnvio(Envio envio, Ruta ruta) {
        this.envio = envio;
        this.ruta = ruta;
    }

    public Envio getEnvio() { return envio; }
    public Ruta getRuta() { return ruta; }

    public void setRuta(Ruta ruta) { this.ruta = ruta; }

    public boolean isLlegaATiempo() { return llegaATiempo; }
    public void setLlegaATiempo(boolean llegaATiempo) { this.llegaATiempo = llegaATiempo; }

    public int getTiempoEntregaMin() { return tiempoEntregaMin; }
    public void setTiempoEntregaMin(int tiempoEntregaMin) { this.tiempoEntregaMin = tiempoEntregaMin; }
}
