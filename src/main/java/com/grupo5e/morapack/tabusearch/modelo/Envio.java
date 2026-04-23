package com.grupo5e.morapack.tabusearch.modelo;

/**
 * Representa un envio (conjunto de maletas) que debe trasladarse desde un
 * aeropuerto origen a un aeropuerto destino.
 *
 * Formato de archivo: _envios_XXXX_.txt donde XXXX es el codigo del aeropuerto
 * origen. Cada linea: id_envio-aaaammdd-hh-mm-DEST-cantidad-id_cliente
 *
 * Campos clave:
 *  - origen: ICAO deducido del NOMBRE del archivo.
 *  - destino: ICAO del campo DEST de la linea.
 *  - instanteGeneracionMinGmt: minuto (en referencia GMT) en que el envio nace.
 *    El envio no puede tomar vuelos que salgan antes de este instante.
 *  - cantidad: numero de maletas a transportar juntas.
 *
 * Esta clase es inmutable. El estado "a que ruta esta asignado" lo mantiene
 * {@link Solucion} via {@link AsignacionEnvio}.
 */
public final class Envio {

    private final String id;
    private final String origen;
    private final String destino;
    private final int instanteGeneracionMinGmt;
    private final int cantidad;

    public Envio(String id, String origen, String destino,
                 int instanteGeneracionMinGmt, int cantidad) {
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.instanteGeneracionMinGmt = instanteGeneracionMinGmt;
        this.cantidad = cantidad;
    }

    public String getId() { return id; }
    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }
    public int getInstanteGeneracionMinGmt() { return instanteGeneracionMinGmt; }
    public int getCantidad() { return cantidad; }

    @Override
    public String toString() {
        return id + " " + origen + "->" + destino + " t=" + instanteGeneracionMinGmt
                + " x" + cantidad;
    }
}
