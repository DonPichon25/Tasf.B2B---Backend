package com.grupo5e.morapack.tabusearch.modelo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Contenedor de la solucion que el TabuSearch mantiene y modifica.
 *
 * Estado:
 *  - asignaciones: una por envio, indexada por id del envio. LinkedHashMap
 *    para preservar el orden de insercion, util al imprimir el reporte.
 *  - valorObjetivo: funcion de coste que el TabuSearch minimiza.
 *      valorObjetivo = enviosTarde * PESO_TARDE
 *                    + retrasoTotalMin * PESO_RETRASO
 *                    + sobreCapacidad * PESO_SOBRECAPACIDAD
 *    Valores mas bajos son mejores.
 *  - enviosEntregadosATiempo / enviosTarde: contadores informativos.
 *
 * Para poder hacer "rollback" o comparar con la mejor solucion, se puede
 * clonar con {@link #copiaSuperficial()}.
 */
public class Solucion {

    private final Map<String, AsignacionEnvio> asignaciones = new LinkedHashMap<>();

    private double valorObjetivo = Double.POSITIVE_INFINITY;
    private int enviosEntregadosATiempo;
    private int enviosTarde;
    private int enviosSinRuta;

    public void agregar(AsignacionEnvio a) {
        asignaciones.put(a.getEnvio().getId(), a);
    }

    public AsignacionEnvio obtener(String idEnvio) {
        return asignaciones.get(idEnvio);
    }

    public List<AsignacionEnvio> lista() {
        return new ArrayList<>(asignaciones.values());
    }

    public int tamano() { return asignaciones.size(); }

    public double getValorObjetivo() { return valorObjetivo; }
    public void setValorObjetivo(double valorObjetivo) { this.valorObjetivo = valorObjetivo; }

    public int getEnviosEntregadosATiempo() { return enviosEntregadosATiempo; }
    public void setEnviosEntregadosATiempo(int enviosEntregadosATiempo) { this.enviosEntregadosATiempo = enviosEntregadosATiempo; }

    public int getEnviosTarde() { return enviosTarde; }
    public void setEnviosTarde(int enviosTarde) { this.enviosTarde = enviosTarde; }

    public int getEnviosSinRuta() { return enviosSinRuta; }
    public void setEnviosSinRuta(int enviosSinRuta) { this.enviosSinRuta = enviosSinRuta; }

    /**
     * Copia superficial: clona el mapa y las asignaciones (pero los objetos
     * Envio y Ruta se comparten, lo cual es seguro porque son inmutables).
     */
    public Solucion copiaSuperficial() {
        Solucion clon = new Solucion();
        for (AsignacionEnvio a : asignaciones.values()) {
            AsignacionEnvio cp = new AsignacionEnvio(a.getEnvio(), a.getRuta());
            cp.setLlegaATiempo(a.isLlegaATiempo());
            cp.setTiempoEntregaMin(a.getTiempoEntregaMin());
            clon.agregar(cp);
        }
        clon.valorObjetivo = this.valorObjetivo;
        clon.enviosEntregadosATiempo = this.enviosEntregadosATiempo;
        clon.enviosTarde = this.enviosTarde;
        clon.enviosSinRuta = this.enviosSinRuta;
        return clon;
    }
}
