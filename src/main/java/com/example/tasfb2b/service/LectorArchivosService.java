package com.example.tasfb2b.service;

import com.example.tasfb2b.model.Aeropuerto;
import com.example.tasfb2b.model.Pedido;
import com.example.tasfb2b.model.Vuelo;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class LectorArchivosService {

    // 1. LECTURA DE AEROPUERTOS (Con Memoria de Continente)
    public List<Aeropuerto> leerAeropuertos(String rutaArchivo) {
        List<Aeropuerto> aeropuertos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            String continenteActual = "DESCONOCIDO";

            while ((linea = br.readLine()) != null) {
                // Eliminamos los bytes nulos fantasmas antes de hacer cualquier otra cosa
                linea = linea.replace("\0", "");

                String lineaTrim = linea.trim();

                // 1. Saltar líneas vacías o cabeceras
                if (lineaTrim.isEmpty() || lineaTrim.startsWith("*") || lineaTrim.startsWith("PDDS") || lineaTrim.startsWith("GMT")) {
                    continue;
                }

                // 2. Detectar Continente
                String min = lineaTrim.toLowerCase();
                if (min.contains("america")) { continenteActual = "AMERICA_DEL_SUR"; continue; }
                if (min.contains("europa")) { continenteActual = "EUROPA"; continue; }
                if (min.contains("asia")) { continenteActual = "ASIA"; continue; }

                // 3. Parsear datos dividiendo por espacios reales
                String[] partes = lineaTrim.split("\\s+");

                // Solo procesar si parece una línea válida
                if (partes.length >= 7 && Character.isDigit(partes[0].charAt(0))) {
                    try {
                        Aeropuerto aero = new Aeropuerto();
                        aero.setContinente(continenteActual);
                        aero.setCodigo(partes[1]); // Ej: SKBO

                        // EL TRUCO DEL ANCLA: Buscar el índice del GMT (empieza con + o -)
                        int indiceTimezone = -1;
                        for (int i = 2; i < partes.length; i++) {
                            if (partes[i].startsWith("+") || partes[i].startsWith("-")) {
                                try {
                                    Integer.parseInt(partes[i]);
                                    indiceTimezone = i;
                                    break;
                                } catch (NumberFormatException e) {
                                    // Falsa alarma, seguimos
                                }
                            }
                        }

                        if (indiceTimezone == -1) {
                            System.err.println("⚠ Saltando línea sin GMT: " + lineaTrim);
                            continue;
                        }

                        // Extraer GMT y Capacidad
                        String gmtStr = partes[indiceTimezone].replace("+", "");
                        aero.setGmt(Integer.parseInt(gmtStr));
                        aero.setCapacidadMax(Integer.parseInt(partes[indiceTimezone + 1]));

                        // Extraer País
                        aero.setPais(partes[indiceTimezone - 2]);

                        // Extraer Ciudad (uniendo palabras)
                        StringBuilder ciudadBuilder = new StringBuilder();
                        for (int i = 2; i < indiceTimezone - 2; i++) {
                            ciudadBuilder.append(partes[i]).append(" ");
                        }
                        aero.setNombre(ciudadBuilder.toString().trim());

                        aeropuertos.add(aero);

                    } catch (Exception e) {
                        System.err.println("❌ Error estructurando aeropuerto: " + lineaTrim + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error crítico en el lector de aeropuertos: " + e.getMessage());
        }
        return aeropuertos;
    }

    // 2. LECTURA DE VUELOS
    public List<Vuelo> leerVuelos(String rutaArchivo) {
        List<Vuelo> vuelos = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.trim().split("-");
                if (partes.length == 5) {
                    Vuelo vuelo = new Vuelo();

                    // Limpieza a los códigos
                    vuelo.setOrigen(partes[0].replaceAll("[^A-Za-z]", "").toUpperCase());
                    vuelo.setDestino(partes[1].replaceAll("[^A-Za-z]", "").toUpperCase());

                    vuelo.setHoraSalida(LocalTime.parse(partes[2]));
                    vuelo.setHoraLlegada(LocalTime.parse(partes[3]));
                    vuelo.setCapacidadMax(Integer.parseInt(partes[4]));

                    vuelos.add(vuelo);
                }
            }
        } catch (Exception e) {
            System.err.println("Error leyendo vuelos: " + e.getMessage());
        }
        return vuelos;
    }

    // 3. LECTURA DE ENVÍOS (Ajustado para evitar duplicados)
    public List<Pedido> leerEnvios(String rutaArchivo) {
        List<Pedido> pedidos = new ArrayList<>();
        DateTimeFormatter formatoFechaHora = DateTimeFormatter.ofPattern("yyyyMMdd-HH-mm");

        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            String nombreArchivo = new File(rutaArchivo).getName();

            // Extraemos el origen del nombre del archivo (ej: _envios_SKBO_.txt)
            String origen = nombreArchivo.split("_")[2].replaceAll("[^A-Za-z]", "").toUpperCase();

            while ((linea = br.readLine()) != null) {
                String[] partes = linea.trim().split("-");
                if (partes.length == 7) {
                    Pedido pedido = new Pedido();

                    // Concatenamos el origen con el número para que sea único en todo el sistema
                    pedido.setIdPedido(origen + "-" + partes[0]);
                    // ----------------------------

                    pedido.setOrigen(origen);
                    pedido.setDestino(partes[4].replaceAll("[^A-Za-z]", "").toUpperCase());

                    String cadenaFechaHora = partes[1] + "-" + partes[2] + "-" + partes[3];
                    pedido.setFechaRegistro(LocalDateTime.parse(cadenaFechaHora, formatoFechaHora));

                    pedido.setCantidadMaletas(Integer.parseInt(partes[5]));
                    pedido.setIdCliente(partes[6]);

                    pedidos.add(pedido);
                }
            }
        } catch (Exception e) {
            System.err.println("Error leyendo pedidos en " + rutaArchivo + ": " + e.getMessage());
        }
        return pedidos;
    }
}