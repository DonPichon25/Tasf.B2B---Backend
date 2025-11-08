package com.grupo5e.morapack.core.validation;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.constants.AlgorithmConstants;

/**
 * HeadquartersValidator - Validador de sedes principales de MoraPack
 * 
 * Valida que los pedidos se originen desde las sedes principales:
 * - Lima, Perú
 * - Bruselas, Bélgica  
 * - Bakú, Azerbaiyán
 */
public class HeadquartersValidator {

    /**
     * Verifica si una ciudad es sede principal de MoraPack
     * 
     * @param ciudad Ciudad a verificar
     * @return true si es Lima, Brussels o Baku
     */
    public static boolean isMoraPackHeadquarters(Ciudad ciudad) {
        if (ciudad == null || ciudad.getNombre() == null) {
            return false;
        }
        
        String cityName = normalizeCityName(ciudad.getNombre());
        
        return cityName.contains("lima") || 
               cityName.contains("brussels") || cityName.contains("bruselas") ||
               cityName.contains("baku") ||
               cityName.contains("peru") ||
               cityName.contains("belgium") ||
               cityName.contains("azerbaijan");
    }

    /**
     * Verifica si un pedido se origina desde una sede principal de MoraPack
     * 
     * @param pedido Pedido a validar
     * @param aeropuertoOrigen Aeropuerto de origen
     * @return true si el origen es una sede válida
     */
    public static boolean isValidOrigin(Pedido pedido, Aeropuerto aeropuertoOrigen) {
        if (aeropuertoOrigen == null || aeropuertoOrigen.getCiudad() == null) {
            return false;
        }
        
        return isMoraPackHeadquarters(aeropuertoOrigen.getCiudad());
    }

    /**
     * Valida el origen de un pedido y lanza excepción si no es válido
     * Solo aplica si VALIDATE_HEADQUARTERS_ORIGIN está habilitado
     * 
     * @param pedido Pedido a validar
     * @param aeropuertoOrigen Aeropuerto de origen
     * @throws IllegalArgumentException si el origen no es válido y la validación está activa
     */
    public static void validateOriginOrThrow(Pedido pedido, Aeropuerto aeropuertoOrigen) {
        if (!AlgorithmConstants.VALIDATE_HEADQUARTERS_ORIGIN) {
            return; // Validación deshabilitada
        }
        
        if (!isValidOrigin(pedido, aeropuertoOrigen)) {
            String origen = aeropuertoOrigen != null && aeropuertoOrigen.getCiudad() != null 
                ? aeropuertoOrigen.getCiudad().getNombre() 
                : "DESCONOCIDO";
            
            throw new IllegalArgumentException(
                String.format("Pedido %d no se origina desde una sede principal de MoraPack. Origen actual: %s. " +
                             "Sedes válidas: Lima (Peru), Brussels (Belgium), Baku (Azerbaijan)", 
                             pedido.getId(), origen)
            );
        }
    }

    /**
     * Valida el origen de un pedido y registra advertencia si no es válido
     * No lanza excepción, solo advierte
     * 
     * @param pedido Pedido a validar
     * @param aeropuertoOrigen Aeropuerto de origen
     * @return true si es válido, false si no lo es (con advertencia)
     */
    public static boolean validateOriginWithWarning(Pedido pedido, Aeropuerto aeropuertoOrigen) {
        if (!AlgorithmConstants.VALIDATE_HEADQUARTERS_ORIGIN) {
            return true; // Validación deshabilitada
        }
        
        boolean isValid = isValidOrigin(pedido, aeropuertoOrigen);
        
        if (!isValid && AlgorithmConstants.VERBOSE_LOGGING) {
            String origen = aeropuertoOrigen != null && aeropuertoOrigen.getCiudad() != null 
                ? aeropuertoOrigen.getCiudad().getNombre() 
                : "DESCONOCIDO";
            
            System.out.println(String.format(
                "⚠️ ADVERTENCIA - Pedido %d no origina desde sede MoraPack: %s", 
                pedido.getId(), origen
            ));
        }
        
        return isValid;
    }

    /**
     * Obtiene la sede MoraPack más cercana a un continente
     * 
     * @param destinationContinent Continente destino
     * @return Nombre de la sede recomendada
     */
    public static String getRecommendedHeadquarters(String destinationContinent) {
        if (destinationContinent == null) {
            return AlgorithmConstants.LIMA_WAREHOUSE;
        }
        
        String continent = destinationContinent.toLowerCase();
        
        if (continent.contains("europe") || continent.contains("europa")) {
            return AlgorithmConstants.BRUSSELS_WAREHOUSE;
        } else if (continent.contains("asia")) {
            return AlgorithmConstants.BAKU_WAREHOUSE;
        } else {
            return AlgorithmConstants.LIMA_WAREHOUSE; // América por defecto
        }
    }

    /**
     * Normaliza el nombre de una ciudad para comparación
     * 
     * @param cityName Nombre de la ciudad
     * @return Nombre normalizado (lowercase, sin espacios extras)
     */
    private static String normalizeCityName(String cityName) {
        if (cityName == null) {
            return "";
        }
        return cityName.trim().toLowerCase();
    }
}

