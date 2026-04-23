package com.grupo5e.morapack.tabusearch.modelo;

/**
 * Representa un vuelo disponible del archivo planes_vuelo.txt.
 *
 * Formato del archivo: ORIGEN-DESTINO-HH:MM-HH:MM-CCCC
 *
 * Las horas del archivo son horas LOCALES de cada aeropuerto.
 * Para poder comparar horarios de distintos aeropuertos (y validar conexiones
 * en rutas multi-tramo), se normalizan ambas horas a una referencia GMT comun.
 *
 * Campos normalizados:
 *  - salidaMinGmt : minutos desde medianoche GMT en que sale el vuelo.
 *  - llegadaMinGmt: minutos desde medianoche GMT en que llega. Si llega al dia
 *    siguiente (por cruzar medianoche al normalizar), se le suma 1440 para
 *    mantener la relacion llegada >= salida.
 *
 * Capacidad: numero maximo de maletas. El TabuSearch debera respetarlo; no
 * podra asignar al mismo vuelo mas maletas de las que soporta.
 *
 * La ocupacion dinamica del vuelo NO se guarda aqui. Se mantiene en una
 * estructura aparte dentro de {@code EvaluadorSolucion} para que varias
 * soluciones candidatas puedan compartir el mismo objeto Vuelo.
 */
public final class Vuelo {

    /** Identificador estable, util para tabla hash y lista tabu. */
    private final int id;
    private final String origen;
    private final String destino;

    /** Hora literal del archivo, para reportes legibles (ej. "23:08"). */
    private final String horaSalidaLocalTxt;
    private final String horaLlegadaLocalTxt;

    /** Minutos desde medianoche en hora LOCAL del aeropuerto. */
    private final int salidaMinLocal;
    private final int llegadaMinLocal;

    /** Minutos desde medianoche referidos a GMT (ya normalizados). */
    private final int salidaMinGmt;
    private final int llegadaMinGmt;

    private final int capacidadMaxima;

    public Vuelo(int id, String origen, String destino,
                 String horaSalidaLocalTxt, String horaLlegadaLocalTxt,
                 int salidaMinLocal, int llegadaMinLocal,
                 int salidaMinGmt, int llegadaMinGmt,
                 int capacidadMaxima) {
        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.horaSalidaLocalTxt = horaSalidaLocalTxt;
        this.horaLlegadaLocalTxt = horaLlegadaLocalTxt;
        this.salidaMinLocal = salidaMinLocal;
        this.llegadaMinLocal = llegadaMinLocal;
        this.salidaMinGmt = salidaMinGmt;
        this.llegadaMinGmt = llegadaMinGmt;
        this.capacidadMaxima = capacidadMaxima;
    }

    public int getId() { return id; }
    public String getOrigen() { return origen; }
    public String getDestino() { return destino; }
    public String getHoraSalidaLocalTxt() { return horaSalidaLocalTxt; }
    public String getHoraLlegadaLocalTxt() { return horaLlegadaLocalTxt; }
    public int getSalidaMinLocal() { return salidaMinLocal; }
    public int getLlegadaMinLocal() { return llegadaMinLocal; }
    public int getSalidaMinGmt() { return salidaMinGmt; }
    public int getLlegadaMinGmt() { return llegadaMinGmt; }
    public int getCapacidadMaxima() { return capacidadMaxima; }

    /** Duracion efectiva del vuelo en minutos segun horarios GMT normalizados. */
    public int duracionMin() {
        return llegadaMinGmt - salidaMinGmt;
    }

    @Override
    public String toString() {
        return origen + "-" + destino + "-" + horaSalidaLocalTxt + "-" + horaLlegadaLocalTxt
                + "-cap" + capacidadMaxima;
    }
}
