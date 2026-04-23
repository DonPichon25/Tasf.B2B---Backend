package com.grupo5e.morapack.tabusearch.io;

import com.grupo5e.morapack.tabusearch.modelo.Aeropuerto;
import com.grupo5e.morapack.tabusearch.modelo.Vuelo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Carga el archivo planes_vuelo.txt en memoria y normaliza horarios a GMT.
 *
 * Cada linea tiene formato:
 *   ORIGEN-DESTINO-HH:MM-HH:MM-CCCC
 *
 * Las horas son locales del aeropuerto correspondiente. Para comparar en una
 * misma referencia usamos el GMT de cada aeropuerto:
 *   salidaMinGmt  = salidaMinLocal  - gmtOrigen  * 60   (modulo 1440)
 *   llegadaMinGmt = llegadaMinLocal - gmtDestino * 60   (modulo 1440)
 *
 * Si tras normalizar llegadaMinGmt < salidaMinGmt, significa que el vuelo
 * llega "al dia siguiente" en la referencia GMT, asi que sumamos 1440 a la
 * llegada para mantener llegada >= salida.
 *
 * Ademas, ofrece un metodo {@link #cargarEnTandas} para leer el archivo de
 * vuelos en bloques de tamano configurable, tal como sugirio el enunciado:
 * cargar una cantidad decente y si no alcanza, pedir otra tanda.
 */
public class CargadorVuelos {

    /**
     * Carga todos los vuelos del archivo y los devuelve.
     *
     * @param ruta ruta al archivo planes_vuelo.txt (UTF-8)
     * @param aeropuertos mapa previamente cargado; necesario para obtener el
     *                    GMT del origen/destino de cada vuelo.
     */
    public List<Vuelo> cargarTodos(Path ruta, Map<String, Aeropuerto> aeropuertos) throws IOException {
        List<String> lineas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        List<Vuelo> vuelos = new ArrayList<>(lineas.size());
        int id = 0;
        for (String linea : lineas) {
            if (linea == null) continue;
            String l = linea.trim();
            if (l.isEmpty()) continue;
            Vuelo v = parsearLinea(l, id, aeropuertos);
            if (v != null) {
                vuelos.add(v);
                id++;
            }
        }
        return vuelos;
    }

    /**
     * Carga vuelos en tandas. Util cuando el numero de vuelos es muy grande y
     * queremos procesar por lotes. Devuelve un "cursor" con el siguiente
     * indice de archivo a leer.
     *
     * Esta version simple lee toda la lista y expone una vista incremental,
     * pero mantiene la misma interfaz que se pediria para streaming real.
     */
    public Tanda cargarEnTandas(Path ruta, Map<String, Aeropuerto> aeropuertos,
                                int tamanoTanda, int desde) throws IOException {
        List<String> todas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        int fin = Math.min(desde + tamanoTanda, todas.size());
        List<Vuelo> vuelos = new ArrayList<>();
        for (int i = desde; i < fin; i++) {
            String l = todas.get(i).trim();
            if (l.isEmpty()) continue;
            Vuelo v = parsearLinea(l, i, aeropuertos);
            if (v != null) vuelos.add(v);
        }
        return new Tanda(vuelos, fin, fin >= todas.size());
    }

    /** Resultado de {@link #cargarEnTandas}. */
    public static final class Tanda {
        public final List<Vuelo> vuelos;
        public final int siguienteIndice;
        public final boolean finArchivo;

        public Tanda(List<Vuelo> vuelos, int siguienteIndice, boolean finArchivo) {
            this.vuelos = vuelos;
            this.siguienteIndice = siguienteIndice;
            this.finArchivo = finArchivo;
        }
    }

    private Vuelo parsearLinea(String linea, int id, Map<String, Aeropuerto> aeropuertos) {
        String[] p = linea.split("-");
        if (p.length != 5) return null;
        try {
            String origen = p[0].trim();
            String destino = p[1].trim();
            String horaSalida = p[2].trim();
            String horaLlegada = p[3].trim();
            int capacidad = Integer.parseInt(p[4].trim());

            Aeropuerto ao = aeropuertos.get(origen);
            Aeropuerto ad = aeropuertos.get(destino);
            if (ao == null || ad == null) return null;

            int salidaLocal = minutosDesdeMedianoche(horaSalida);
            int llegadaLocal = minutosDesdeMedianoche(horaLlegada);
            if (salidaLocal < 0 || llegadaLocal < 0) return null;

            int salidaGmt = ((salidaLocal - ao.getGmt() * 60) % 1440 + 1440) % 1440;
            int llegadaGmt = ((llegadaLocal - ad.getGmt() * 60) % 1440 + 1440) % 1440;
            if (llegadaGmt < salidaGmt) llegadaGmt += 1440;

            return new Vuelo(id, origen, destino, horaSalida, horaLlegada,
                    salidaLocal, llegadaLocal, salidaGmt, llegadaGmt, capacidad);
        } catch (Exception e) {
            return null;
        }
    }

    private int minutosDesdeMedianoche(String hhmm) {
        String[] p = hhmm.split(":");
        if (p.length != 2) return -1;
        int h = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        return h * 60 + m;
    }
}
