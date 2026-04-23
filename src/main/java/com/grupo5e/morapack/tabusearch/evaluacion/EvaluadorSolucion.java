package com.grupo5e.morapack.tabusearch.evaluacion;

import com.grupo5e.morapack.tabusearch.algoritmo.EstadoCapacidades;
import com.grupo5e.morapack.tabusearch.config.ConfiguracionTabu;
import com.grupo5e.morapack.tabusearch.modelo.Aeropuerto;
import com.grupo5e.morapack.tabusearch.modelo.AsignacionEnvio;
import com.grupo5e.morapack.tabusearch.modelo.Envio;
import com.grupo5e.morapack.tabusearch.modelo.Ruta;
import com.grupo5e.morapack.tabusearch.modelo.Solucion;
import com.grupo5e.morapack.tabusearch.modelo.Vuelo;

import java.util.Map;

/**
 * Calcula el valor objetivo de una solucion y decora cada asignacion con:
 *  - si llega a tiempo
 *  - tiempo total de entrega (minutos desde el instante de envio hasta la
 *    liberacion en el almacen destino).
 *
 * Funcion objetivo minimizada:
 *   f(S) = pesoSinRuta * envios_sin_ruta
 *        + pesoEnvioTarde * envios_tarde
 *        + pesoMinutoRetraso * sum(min_retraso)
 *        + pesoMinutoEnRed  * sum(min_en_red)
 *
 * Ademas valida la feasibilidad de cada ruta: si la ruta tiene inconsistencias
 * basicas (origen/destino o conexiones temporales erroneas) se marca como
 * no-viable (rutaInvalida).
 */
public class EvaluadorSolucion {

    private final Map<String, Aeropuerto> aeropuertos;
    private final ConfiguracionTabu cfg;

    public EvaluadorSolucion(Map<String, Aeropuerto> aeropuertos, ConfiguracionTabu cfg) {
        this.aeropuertos = aeropuertos;
        this.cfg = cfg;
    }

    /**
     * Evalua una solucion completa, usando opcionalmente un estado de
     * capacidades para penalizar sobrecarga.
     */
    public void evaluar(Solucion s, EstadoCapacidades caps) {
        int tarde = 0;
        int aTiempo = 0;
        int sinRuta = 0;
        double sumaRetraso = 0.0;
        double sumaEnRed = 0.0;

        for (AsignacionEnvio a : s.lista()) {
            Envio e = a.getEnvio();
            Ruta r = a.getRuta();

            if (r == null || r.esVacia() || !rutaCoherente(e, r)) {
                a.setLlegaATiempo(false);
                a.setTiempoEntregaMin(Integer.MAX_VALUE);
                sinRuta++;
                continue;
            }

            int llegadaFinal = r.minutoLlegada();
            int tiempoEnRed = (llegadaFinal + cfg.tiempoLiberacionDestinoMin)
                    - e.getInstanteGeneracionMinGmt();
            if (tiempoEnRed < 0) tiempoEnRed = 0;

            int plazo = calcularPlazo(e);
            boolean ok = tiempoEnRed <= plazo;
            a.setLlegaATiempo(ok);
            a.setTiempoEntregaMin(tiempoEnRed);

            if (ok) {
                aTiempo++;
            } else {
                tarde++;
                sumaRetraso += (tiempoEnRed - plazo);
            }
            sumaEnRed += tiempoEnRed;
        }

        double v = cfg.pesoSinRuta * sinRuta
                + cfg.pesoEnvioTarde * tarde
                + cfg.pesoMinutoRetraso * sumaRetraso
                + cfg.pesoMinutoEnRed * sumaEnRed;

        s.setEnviosSinRuta(sinRuta);
        s.setEnviosTarde(tarde);
        s.setEnviosEntregadosATiempo(aTiempo);
        s.setValorObjetivo(v);
    }

    /**
     * Valida que la ruta conecte origen con destino y que cada tramo respete
     * el tiempo de escala. No chequea capacidades aqui (eso lo hace el caller).
     */
    public boolean rutaCoherente(Envio e, Ruta r) {
        if (r == null || r.esVacia()) return false;
        if (!r.getVuelos().get(0).getOrigen().equals(e.getOrigen())) return false;
        if (!r.getVuelos().get(r.getVuelos().size() - 1).getDestino().equals(e.getDestino())) return false;

        int minActual = e.getInstanteGeneracionMinGmt();
        for (int i = 0; i < r.getVuelos().size(); i++) {
            Vuelo v = r.getVuelos().get(i);
            int minSalMin = (i == 0 ? minActual : minActual + cfg.tiempoEscalaMin);
            if (v.getSalidaMinGmt() < minSalMin) return false;
            if (i > 0 && !v.getOrigen().equals(r.getVuelos().get(i - 1).getDestino())) return false;
            minActual = v.getLlegadaMinGmt();
        }
        return true;
    }

    /** Devuelve el plazo maximo segun si es mismo o distinto continente. */
    public int calcularPlazo(Envio e) {
        Aeropuerto o = aeropuertos.get(e.getOrigen());
        Aeropuerto d = aeropuertos.get(e.getDestino());
        if (o == null || d == null) return cfg.plazoDistintoContinenteMin;
        return o.getContinente().mismoQue(d.getContinente())
                ? cfg.plazoMismoContinenteMin
                : cfg.plazoDistintoContinenteMin;
    }
}
