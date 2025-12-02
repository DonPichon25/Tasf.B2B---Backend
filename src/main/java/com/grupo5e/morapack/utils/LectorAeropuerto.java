package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Almacen;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.enums.Continente;
import com.grupo5e.morapack.core.enums.EstadoAeropuerto;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Lector de aeropuertos basado en el formato del archivo airportInfo.txt
 * Adaptado del ejemplo MoraPack-Backend/InputAirports.java
 */
public class LectorAeropuerto {

    private ArrayList<Aeropuerto> aeropuertos;
    private final String rutaArchivo;

    public LectorAeropuerto(String rutaArchivo) {
        this.rutaArchivo = rutaArchivo;
        this.aeropuertos = new ArrayList<>();
    }

    public ArrayList<Aeropuerto> leerAeropuertos() {
        try (BufferedReader reader = new BufferedReader(new FileReader(rutaArchivo))) {
            String linea;
            Continente continenteActual = null;
            Map<String, Ciudad> mapaCiudades = new HashMap<>();

            // Saltar las primeras dos líneas (header)
            reader.readLine();
            reader.readLine();

            while ((linea = reader.readLine()) != null) {
                
                // Saltar líneas vacías
                if (linea.trim().isEmpty()) {
                    continue;
                }
                
                // Verificar si es una línea de header de continente
                if (linea.contains("America") || linea.contains("Europa") || linea.contains("Asia")) {
                    if (linea.contains("America")) {
                        continenteActual = Continente.AMERICA;
                        System.out.println("✓ Continente: " + continenteActual);
                    } else if (linea.contains("Europa")) {
                        continenteActual = Continente.EUROPA;
                        System.out.println("✓ Continente: " + continenteActual);
                    } else if (linea.contains("Asia")) {
                        continenteActual = Continente.ASIA;
                        System.out.println("✓ Continente: " + continenteActual);
                    }
                    continue;
                }
                
                // Parsear datos del aeropuerto
                String[] partes = linea.trim().split("\\s+");

                if (partes.length >= 7) {
                    // Declarar variables fuera del try para que sean accesibles después
                    int id = 0;
                    String codigoIATA = "";
                    String nombreCiudad = "";
                    String nombrePais = "";
                    String alias = "";
                    int zonaHoraria = 0;
                    double capacidadMaxima = 400.0;

                    try {
                        id = Integer.parseInt(partes[0]);
                        codigoIATA = partes[1];

                        // Buscar timezone (empieza con + o -)
                        int indiceTimezone = -1;
                        for (int i = 2; i < partes.length; i++) {
                            if (partes[i].startsWith("+") || partes[i].startsWith("-")) {
                                // Verificar si es un número (timezone) y no parte del nombre de ciudad
                                try {
                                    Integer.parseInt(partes[i]);
                                    indiceTimezone = i;
                                    break;
                                } catch (NumberFormatException e) {
                                    // No es un timezone, continuar buscando
                                }
                            }
                        }

                        if (indiceTimezone == -1) {
                            System.err.println("⚠ Advertencia: No se pudo encontrar timezone para " + codigoIATA + ", saltando");
                            continue;
                        }

                        // Extraer nombre de ciudad (de partes[2] hasta indiceTimezone-3)
                        // Formato: NombreCiudad Pais alias timezone
                        nombreCiudad = partes[2];

                        // Extraer país (una posición antes del alias)
                        nombrePais = partes[indiceTimezone - 2];

                        // Extraer alias (una posición antes del timezone)
                        alias = partes[indiceTimezone - 1];

                        // Extraer timezone
                        zonaHoraria = Integer.parseInt(partes[indiceTimezone]);

                        // Extraer capacidad (una posición después del timezone)
                        int indiceCapacidad = indiceTimezone + 1;
                        capacidadMaxima = 400.0; // Por defecto
                        if (indiceCapacidad < partes.length && !partes[indiceCapacidad].equals("Latitude:")) {
                            try {
                                capacidadMaxima = Double.parseDouble(partes[indiceCapacidad]);
                            } catch (NumberFormatException e) {
                                System.out.println("⚠ Advertencia: No se pudo parsear capacidad para " + codigoIATA + ", usando 400.0");
                            }
                        }

                        System.out.println("✓ Parseado: ID=" + id + ", IATA=" + codigoIATA + ", Ciudad=" + nombreCiudad +
                                         ", País=" + nombrePais + ", Alias=" + alias +
                                         ", TZ=" + zonaHoraria + ", Cap=" + capacidadMaxima);
                    } catch (Exception e) {
                        System.err.println("❌ Error parseando línea: " + linea);
                        System.err.println("❌ Error: " + e.getMessage());
                        continue;
                    }

                    // Extraer latitud y longitud
                    String latitudStr = "0.0";
                    String longitudStr = "0.0";

                    // Buscar latitud y longitud en la línea
                    int indiceLat = linea.indexOf("Latitude:");
                    int indiceLong = linea.indexOf("Longitude:");

                    if (indiceLat != -1 && indiceLong != -1) {
                        String latRaw = linea.substring(indiceLat + 10, indiceLong).trim();
                        String longRaw = linea.substring(indiceLong + 11).trim();

                        // Convertir DMS (Grados Minutos Segundos) a decimal
                        latitudStr = convertirDMSADecimal(latRaw);
                        longitudStr = convertirDMSADecimal(longRaw);
                    }
                    
                    // Crear objeto Ciudad si no existe
                    String claveCiudad = nombreCiudad + "-" + nombrePais;
                    Ciudad ciudad = mapaCiudades.get(claveCiudad);
                    if (ciudad == null) {
                        ciudad = new Ciudad();
                        // NO setear el ID manualmente, Hibernate lo genera automáticamente
                        ciudad.setCodigo(alias.toUpperCase());  // Código de ciudad (ej: "QUIT", "LIMA")
                        ciudad.setNombre(nombreCiudad);
                        ciudad.setPais(nombrePais);  // País (ej: "Ecuador", "Perú")
                        ciudad.setContinente(continenteActual);
                        ciudad.setTipoData(1);
                        mapaCiudades.put(claveCiudad, ciudad);
                    }
                    
                    // Crear Almacén para el aeropuerto
                    Almacen almacen = new Almacen();
                    // NO setear el ID manualmente, Hibernate lo genera automáticamente
                    almacen.setCapacidadMaxima((int)capacidadMaxima);
                    almacen.setCapacidadUsada(0);
                    almacen.setNombre(nombreCiudad + " Almacén");
                    almacen.setEsAlmacenPrincipal(false);
                    almacen.setTipoData(1);
                    
                    // Crear objeto Aeropuerto
                    Aeropuerto aeropuerto = new Aeropuerto();
                    aeropuerto.setCodigoIATA(codigoIATA);
                    aeropuerto.setZonaHorariaUTC(zonaHoraria);
                    aeropuerto.setLatitud(latitudStr);
                    aeropuerto.setLongitud(longitudStr);
                    aeropuerto.setCiudad(ciudad);
                    aeropuerto.setEstado(EstadoAeropuerto.DISPONIBLE);
                    aeropuerto.setAlmacen(almacen);
                    
                    // Establecer referencia circular
                    almacen.setAeropuerto(aeropuerto);
                    aeropuerto.setTipoData(1);
                    aeropuertos.add(aeropuerto);
                }
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error leyendo datos de aeropuertos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return aeropuertos;
    }

    /**
     * Convierte formato DMS (Grados Minutos Segundos) a grados decimales
     * Ejemplo entrada: "04° 42' 05" N" o "74° 08' 49" W"
     * Ejemplo salida: "4.7014" o "-74.1469"
     */
    private String convertirDMSADecimal(String dmsCadena) {
        try {
            // Limpiar la cadena y dividir por espacios
            String limpia = dmsCadena.replaceAll("[°'\"]+", " ").trim();
            String[] partes = limpia.split("\\s+");

            if (partes.length < 3) {
                System.err.println("⚠ Advertencia: Formato DMS inválido: " + dmsCadena);
                return "0.0";
            }

            // Parsear grados, minutos, segundos
            double grados = Double.parseDouble(partes[0]);
            double minutos = partes.length > 1 ? Double.parseDouble(partes[1]) : 0.0;
            double segundos = partes.length > 2 ? Double.parseDouble(partes[2]) : 0.0;

            // Convertir a decimal
            double decimal = grados + (minutos / 60.0) + (segundos / 3600.0);

            // Verificar si es Sur o Oeste (negativo)
            String direccion = partes.length > 3 ? partes[3] : "";
            if (direccion.equals("S") || direccion.equals("W")) {
                decimal = -decimal;
            }

            // Formatear a 4 decimales
            return String.format("%.4f", decimal);

        } catch (Exception e) {
            System.err.println("❌ Error parseando coordenadas DMS: " + dmsCadena + " - " + e.getMessage());
            return "0.0";
        }
    }
}
