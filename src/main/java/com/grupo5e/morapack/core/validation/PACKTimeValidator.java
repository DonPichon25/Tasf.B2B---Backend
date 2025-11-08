package com.grupo5e.morapack.core.validation;

import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.constants.AlgorithmConstants;

/**
 * PACKTimeValidator - Validador de tiempos de transporte PACK
 * 
 * Valida que los vuelos cumplan con los acuerdos PACK:
 * - Mismo continente: 0.5 días (12 horas)
 * - Diferente continente: 1 día (24 horas)
 */
public class PACKTimeValidator {

    private static final double TOLERANCE_PERCENTAGE = 0.1; // 10% tolerancia

    /**
     * Valida que un vuelo cumpla con los tiempos de transporte PACK
     * 
     * @param vuelo Vuelo a validar
     * @return true si cumple con los tiempos PACK (con 10% tolerancia)
     */
    public static boolean validatePACKTransportTimes(Vuelo vuelo) {
        if (vuelo == null || 
            vuelo.getAeropuertoOrigen() == null || 
            vuelo.getAeropuertoDestino() == null ||
            vuelo.getAeropuertoOrigen().getCiudad() == null || 
            vuelo.getAeropuertoDestino().getCiudad() == null) {
            return false;
        }

        boolean sameContinentFlight = vuelo.getAeropuertoOrigen().getCiudad().getContinente()
                                          .equals(vuelo.getAeropuertoDestino().getCiudad().getContinente());

        double expectedTimeDays = sameContinentFlight ? 
                                 AlgorithmConstants.SAME_CONTINENT_TRANSPORT_TIME : // 0.5 días
                                 AlgorithmConstants.DIFFERENT_CONTINENT_TRANSPORT_TIME; // 1.0 día

        // Permitir variación del ±10% en los tiempos
        double tolerance = expectedTimeDays * TOLERANCE_PERCENTAGE;
        double actualTimeDays = vuelo.getTiempoTransporte() / 24.0; // Convertir horas a días

        boolean isValid = Math.abs(actualTimeDays - expectedTimeDays) <= tolerance;

        if (!isValid && AlgorithmConstants.VERBOSE_LOGGING) {
            System.out.println(String.format(
                "⚠️ ADVERTENCIA - Vuelo %d (%s → %s) no cumple tiempos PACK: " +
                "Esperado: %.2f días, Actual: %.2f días (±%.0f%% tolerancia)",
                vuelo.getId(),
                vuelo.getAeropuertoOrigen().getCodigoIATA(),
                vuelo.getAeropuertoDestino().getCodigoIATA(),
                expectedTimeDays,
                actualTimeDays,
                TOLERANCE_PERCENTAGE * 100
            ));
        }

        return isValid;
    }

    /**
     * Obtiene el tiempo de transporte esperado según PACK
     * 
     * @param sameContinentFlight true si es mismo continente
     * @return Tiempo esperado en días
     */
    public static double getExpectedPACKTime(boolean sameContinentFlight) {
        return sameContinentFlight ? 
               AlgorithmConstants.SAME_CONTINENT_TRANSPORT_TIME : 
               AlgorithmConstants.DIFFERENT_CONTINENT_TRANSPORT_TIME;
    }

    /**
     * Verifica si el tiempo de un vuelo está dentro del rango PACK
     * 
     * @param tiempoTransporteHoras Tiempo de transporte en horas
     * @param sameContinentFlight true si es mismo continente
     * @return true si está dentro del rango permitido
     */
    public static boolean isWithinPACKRange(double tiempoTransporteHoras, boolean sameContinentFlight) {
        double expectedTimeDays = getExpectedPACKTime(sameContinentFlight);
        double tolerance = expectedTimeDays * TOLERANCE_PERCENTAGE;
        double actualTimeDays = tiempoTransporteHoras / 24.0;

        return Math.abs(actualTimeDays - expectedTimeDays) <= tolerance;
    }

    /**
     * Valida capacidad de vuelo según continente
     * 
     * @param vuelo Vuelo a validar
     * @return true si la capacidad cumple con los rangos PACK
     */
    public static boolean validatePACKCapacity(Vuelo vuelo) {
        if (vuelo == null || 
            vuelo.getAeropuertoOrigen() == null || 
            vuelo.getAeropuertoDestino() == null ||
            vuelo.getAeropuertoOrigen().getCiudad() == null || 
            vuelo.getAeropuertoDestino().getCiudad() == null) {
            return false;
        }

        boolean sameContinentFlight = vuelo.getAeropuertoOrigen().getCiudad().getContinente()
                                          .equals(vuelo.getAeropuertoDestino().getCiudad().getContinente());

        int minCapacity, maxCapacity;
        if (sameContinentFlight) {
            minCapacity = AlgorithmConstants.SAME_CONTINENT_MIN_CAPACITY;
            maxCapacity = AlgorithmConstants.SAME_CONTINENT_MAX_CAPACITY;
        } else {
            minCapacity = AlgorithmConstants.DIFFERENT_CONTINENT_MIN_CAPACITY;
            maxCapacity = AlgorithmConstants.DIFFERENT_CONTINENT_MAX_CAPACITY;
        }

        boolean isValid = vuelo.getCapacidadMaxima() >= minCapacity && 
                         vuelo.getCapacidadMaxima() <= maxCapacity;

        if (!isValid && AlgorithmConstants.VERBOSE_LOGGING) {
            System.out.println(String.format(
                "⚠️ ADVERTENCIA - Vuelo %d (%s → %s) capacidad fuera de rango PACK: " +
                "Esperado: %d-%d, Actual: %d",
                vuelo.getId(),
                vuelo.getAeropuertoOrigen().getCodigoIATA(),
                vuelo.getAeropuertoDestino().getCodigoIATA(),
                minCapacity,
                maxCapacity,
                vuelo.getCapacidadMaxima()
            ));
        }

        return isValid;
    }

    /**
     * Validación completa de un vuelo según estándares PACK
     * 
     * @param vuelo Vuelo a validar
     * @return true si cumple con tiempos Y capacidad PACK
     */
    public static boolean validateFullPACKCompliance(Vuelo vuelo) {
        return validatePACKTransportTimes(vuelo) && validatePACKCapacity(vuelo);
    }
}

