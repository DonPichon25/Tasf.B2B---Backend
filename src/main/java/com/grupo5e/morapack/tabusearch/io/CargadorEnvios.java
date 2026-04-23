package com.grupo5e.morapack.tabusearch.io;

import com.grupo5e.morapack.tabusearch.modelo.Aeropuerto;
import com.grupo5e.morapack.tabusearch.modelo.Envio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Carga los envios desde archivos _envios_XXXX_.txt (uno por aeropuerto origen).
 *
 * Formato de cada linea:
 *   id_envio-aaaammdd-hh-mm-DEST-cantidad-id_cliente
 *
 * El codigo de origen NO esta en la linea: se deduce del nombre del archivo
 * (ej. _envios_SPIM_.txt -> origen = SPIM).
 *
 * Politica:
 *  - Se respeta un limite por archivo (envios_por_archivo) y un limite global.
 *  - El "instante" del envio se calcula como minutos desde una fecha base.
 *    Para simplificar, usamos el dia de la primera linea como "dia 0" y se
 *    calcula el instante en minutos desde las 00:00 GMT de ese dia.
 *  - Se toma en cuenta el GMT del aeropuerto origen para normalizar la hora
 *    local declarada.
 */
public class CargadorEnvios {

    /**
     * Resultado de la carga: lista de envios + fecha base en yyyymmdd.
     */
    public static final class Resultado {
        public final List<Envio> envios;
        public final String fechaBase;

        public Resultado(List<Envio> envios, String fechaBase) {
            this.envios = envios;
            this.fechaBase = fechaBase;
        }
    }

    /**
     * Carga todos los archivos _envios_XXXX_.txt del directorio.
     *
     * @param dirEnvios        carpeta que contiene los 30 archivos.
     * @param aeropuertos      mapa de aeropuertos ya cargado.
     * @param maxPorArchivo    limite por archivo (anti-explosion de memoria).
     * @param limiteGlobal     limite global de envios en toda la corrida.
     */
    public Resultado cargar(Path dirEnvios, Map<String, Aeropuerto> aeropuertos,
                            int maxPorArchivo, int limiteGlobal) throws IOException {

        List<Envio> envios = new ArrayList<>();
        String fechaBase = null;
        int totalCargados = 0;

        try (DirectoryStream<Path> ds =
                     Files.newDirectoryStream(dirEnvios, "_envios_*_.txt")) {
            for (Path archivo : ds) {
                if (totalCargados >= limiteGlobal) break;

                String nombre = archivo.getFileName().toString();
                // _envios_SPIM_.txt -> SPIM
                String codigoOrigen = extraerCodigoOrigen(nombre);
                if (codigoOrigen == null) continue;

                Aeropuerto origen = aeropuertos.get(codigoOrigen);
                if (origen == null) continue;

                int leidosEnArchivo = 0;
                for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                    if (leidosEnArchivo >= maxPorArchivo) break;
                    if (totalCargados >= limiteGlobal) break;
                    String l = linea.trim();
                    if (l.isEmpty()) continue;

                    Envio e = parsearLinea(l, origen);
                    if (e == null) continue;

                    if (fechaBase == null) {
                        // Primera linea valida: guardamos la fecha como "dia 0"
                        fechaBase = extraerFecha(l);
                    }

                    envios.add(e);
                    leidosEnArchivo++;
                    totalCargados++;
                }
            }
        }
        return new Resultado(envios, fechaBase == null ? "" : fechaBase);
    }

    private String extraerCodigoOrigen(String nombre) {
        // nombre: _envios_XXXX_.txt
        int i1 = nombre.indexOf("_envios_");
        if (i1 < 0) return null;
        int i2 = nombre.indexOf("_", i1 + 8);
        if (i2 < 0) return null;
        String codigo = nombre.substring(i1 + 8, i2);
        if (codigo.length() != 4) return null;
        return codigo;
    }

    private String extraerFecha(String linea) {
        String[] p = linea.split("-");
        if (p.length >= 2) return p[1];
        return null;
    }

    /**
     * Parsea: 000000001-20260102-01-06-SPIM-002-0001291
     *         id        fecha    hh mm dest cnt  cliente
     * Calcula el instante en minutos GMT desde medianoche de la fecha.
     * Como usamos una fecha base compartida, todas las entradas se expresan
     * relativas a esa fecha.
     */
    private Envio parsearLinea(String l, Aeropuerto origen) {
        String[] p = l.split("-");
        if (p.length < 7) return null;
        try {
            String id = p[0];
            // p[1] fecha
            int hh = Integer.parseInt(p[2]);
            int mm = Integer.parseInt(p[3]);
            String destino = p[4];
            int cantidad = Integer.parseInt(p[5]);
            // p[6] id_cliente no se usa

            int localMin = hh * 60 + mm;
            int gmtMin = ((localMin - origen.getGmt() * 60) % 1440 + 1440) % 1440;

            return new Envio(id, origen.getCodigo(), destino, gmtMin, cantidad);
        } catch (Exception ex) {
            return null;
        }
    }
}
