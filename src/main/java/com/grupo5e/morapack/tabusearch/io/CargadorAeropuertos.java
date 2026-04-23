package com.grupo5e.morapack.tabusearch.io;

import com.grupo5e.morapack.tabusearch.modelo.Aeropuerto;
import com.grupo5e.morapack.tabusearch.modelo.Continente;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lee el archivo aeropuertos.txt (codificacion UTF-16 BE con BOM).
 *
 * El archivo tiene cabecera de 3 lineas y separadores de continente que
 * debemos distinguir de las lineas con datos.
 *
 * Formato de cada linea con datos (separadores multiples espacios o tabs):
 *   NN   CODIGO   Ciudad   Pais   alias   GMT   capacidad   Latitude: ... Longitude: ...
 *
 * Estrategia de parseo: una vez decodificado a UTF-8 en memoria, se intenta
 * capturar codigo (4 letras), pais, GMT (-5 / +3) y capacidad. Ciudad puede
 * tener espacios, se toma la subcadena entre codigo y pais.
 *
 * Salida: Map<codigoICAO, Aeropuerto> preservando orden de lectura.
 */
public class CargadorAeropuertos {

    /**
     * Carga los aeropuertos y los agrupa en continentes segun la seccion donde
     * aparecen en el archivo.
     *
     * @param ruta ruta absoluta o relativa al archivo aeropuertos.txt
     * @return mapa codigo -> Aeropuerto
     */
    public Map<String, Aeropuerto> cargar(Path ruta) throws IOException {
        Map<String, Aeropuerto> aeropuertos = new LinkedHashMap<>();

        // Decodificar UTF-16 BE y releer a String
        byte[] bytes = Files.readAllBytes(ruta);
        String contenido = new String(bytes, StandardCharsets.UTF_16BE);
        // Limpiar BOMs intermedios y caracteres de control que aparecen a veces
        contenido = contenido.replace("\uFEFF", "").replace("\u0000", "");

        Continente continenteActual = null;
        for (String linea : contenido.split("\\r?\\n")) {
            String l = linea.trim();
            if (l.isEmpty()) continue;

            // Ignorar cabeceras y separadores
            if (l.startsWith("*")) continue;
            if (l.contains("PDDS") || l.startsWith("GMT")) continue;

            // Detectar seccion de continente
            String lower = l.toLowerCase();
            if (lower.contains("america del sur")) {
                continenteActual = Continente.AMERICA_DEL_SUR;
                continue;
            }
            if (lower.startsWith("europa")) {
                continenteActual = Continente.EUROPA;
                continue;
            }
            if (lower.startsWith("asia")) {
                continenteActual = Continente.ASIA;
                continue;
            }
            if (lower.contains("capacidad")) continue; // cabecera con "GMT CAPACIDAD"

            // Procesar linea con datos (empieza por numero)
            if (!Character.isDigit(l.charAt(0))) continue;

            Aeropuerto a = parsearLinea(l, continenteActual);
            if (a != null) {
                aeropuertos.put(a.getCodigo(), a);
            }
        }
        return aeropuertos;
    }

    /**
     * Parsea una linea de aeropuerto. Se aprovecha de que el codigo ICAO tiene
     * siempre 4 letras mayusculas y aparece justo despues del numero.
     */
    private Aeropuerto parsearLinea(String linea, Continente cont) {
        // Separar por multiples espacios / tabs
        String[] t = linea.split("\\s+");
        if (t.length < 7) return null;
        try {
            // t[0]=id, t[1]=CODIGO, ... luego Ciudad puede ocupar varios tokens
            String codigo = t[1];
            if (codigo.length() != 4) return null;

            // Buscar GMT: token con sufijo signo o con primer simbolo + o -
            int idxGmt = -1;
            for (int i = 2; i < t.length; i++) {
                String s = t[i];
                if (s.matches("[+\\-]?\\d{1,2}") && (s.startsWith("+") || s.startsWith("-") || i >= 4)) {
                    // Heuristica: el GMT suele ir despues de alias (4 letras minusculas).
                    // Probamos a convertir y descartamos valores absurdos.
                    int valor;
                    try {
                        valor = Integer.parseInt(s);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    if (valor >= -12 && valor <= 14) {
                        idxGmt = i;
                        break;
                    }
                }
            }
            if (idxGmt < 0 || idxGmt + 1 >= t.length) return null;

            int gmt = Integer.parseInt(t[idxGmt]);
            int capacidad = Integer.parseInt(t[idxGmt + 1]);

            // Pais: un token antes del alias (alias tiene 4 letras minusculas, sin acentos).
            // El alias aparece justo antes del GMT (idxGmt - 1).
            int idxAlias = idxGmt - 1;
            int idxPais = idxAlias - 1;
            if (idxPais < 2) return null;
            String pais = t[idxPais];

            // Ciudad = tokens entre t[2] y t[idxPais - 1] (inclusive).
            StringBuilder ciudadSb = new StringBuilder();
            for (int i = 2; i < idxPais; i++) {
                if (ciudadSb.length() > 0) ciudadSb.append(' ');
                ciudadSb.append(t[i]);
            }
            String ciudad = ciudadSb.toString().trim();

            return new Aeropuerto(codigo, ciudad, pais, cont, gmt, capacidad);
        } catch (Exception e) {
            return null;
        }
    }
}
