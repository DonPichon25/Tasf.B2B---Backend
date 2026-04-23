package com.grupo5e.morapack.tabusearch.modelo;

/**
 * Representa un aeropuerto del caso TASF-B2B.
 *
 * Cada aeropuerto tiene:
 *  - codigo ICAO (4 letras), clave primaria.
 *  - ciudad / pais informativos.
 *  - continente: decide el plazo de entrega al combinarlo con el destino.
 *  - gmt: desfase horario entero (por ejemplo -5 o +3). Se usa para normalizar
 *    las horas locales de los vuelos a una referencia comun.
 *  - capacidadMaxima: cantidad maxima de maletas que puede almacenar
 *    simultaneamente el aeropuerto. Se consulta al asignar un envio porque
 *    el TabuSearch debe evitar sobrecargar el almacen.
 *
 * Esta clase es inmutable una vez construida (todos los campos son final).
 * La ocupacion dinamica del almacen se rastrea aparte, en el evaluador de
 * soluciones, para no acoplar la representacion de datos a una corrida.
 */
public final class Aeropuerto {

    private final String codigo;
    private final String ciudad;
    private final String pais;
    private final Continente continente;
    private final int gmt;
    private final int capacidadMaxima;

    public Aeropuerto(String codigo, String ciudad, String pais,
                      Continente continente, int gmt, int capacidadMaxima) {
        this.codigo = codigo;
        this.ciudad = ciudad;
        this.pais = pais;
        this.continente = continente;
        this.gmt = gmt;
        this.capacidadMaxima = capacidadMaxima;
    }

    public String getCodigo() { return codigo; }
    public String getCiudad() { return ciudad; }
    public String getPais() { return pais; }
    public Continente getContinente() { return continente; }
    public int getGmt() { return gmt; }
    public int getCapacidadMaxima() { return capacidadMaxima; }

    @Override
    public String toString() {
        return codigo + " (" + ciudad + ", " + pais + ", " + continente
                + ", GMT" + (gmt >= 0 ? "+" + gmt : gmt)
                + ", cap=" + capacidadMaxima + ")";
    }
}
