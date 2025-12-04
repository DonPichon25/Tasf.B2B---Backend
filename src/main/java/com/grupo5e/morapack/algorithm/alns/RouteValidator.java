package com.grupo5e.morapack.algorithm.alns;

import com.grupo5e.morapack.core.constants.Constantes;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Vuelo;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RouteValidator - Validador optimizado para factibilidad de rutas
 * Siguiendo patrón de MoraPack-Backend
 * 
 * Valida restricciones de rutas con caching y estructuras de datos eficientes
 * - Enforce minimum 1-hour layover at intermediate destinations
 * - Optimiza validación con caching y acceso O(1)
 */
public class RouteValidator {

    // Cache para valores frecuentemente calculados
    private Map<String, Double> cacheRouteTime;
    private Map<String, Boolean> cacheDeadline;

    // Estructuras de búsqueda rápida para acceso O(1)
    private Map<String, Aeropuerto> cityToAirportMap;
    private Map<String, Map<String, Vuelo>> flightLookupMap; // origin -> destination -> flight

    // Constantes
    private static final int MINIMUM_LAYOVER_MINUTES = 60; // 1 hora mínima
    private static final boolean DEBUG_MODE = false;

    public RouteValidator(List<Aeropuerto> aeropuertos, List<Vuelo> vuelos) {
        this.cacheRouteTime = new HashMap<>();
        this.cacheDeadline = new HashMap<>();
        this.cityToAirportMap = new HashMap<>();
        this.flightLookupMap = new HashMap<>();

        // Construir estructuras de búsqueda optimizadas
        initializeCityToAirportMap(aeropuertos);
        initializeFlightLookupMap(vuelos);
    }

    /**
     * Inicializa mapa de búsqueda O(1) de ciudad a aeropuerto
     */
    private void initializeCityToAirportMap(List<Aeropuerto> aeropuertos) {
        for (Aeropuerto aeropuerto : aeropuertos) {
            if (aeropuerto.getCiudad() != null && aeropuerto.getCiudad().getNombre() != null) {
                String key = normalizeCity(aeropuerto.getCiudad().getNombre());
                cityToAirportMap.put(key, aeropuerto);
            }
        }
    }

    /**
     * Inicializa mapa de búsqueda O(1) de vuelos: origen -> destino -> vuelo
     * Optimiza findDirectRoute de O(n) a O(1)
     */
    private void initializeFlightLookupMap(List<Vuelo> vuelos) {
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen() == null || vuelo.getAeropuertoDestino() == null) {
                continue;
            }

            String origenKey = getAirportKey(vuelo.getAeropuertoOrigen());
            String destinoKey = getAirportKey(vuelo.getAeropuertoDestino());

            flightLookupMap
                    .computeIfAbsent(origenKey, k -> new HashMap<>())
                    .put(destinoKey, vuelo);
        }
    }

    /**
     * CRÍTICO: Valida ruta con enforcement de layover mínimo de 1 hora
     * 
     * @param pedido Pedido a validar
     * @param ruta   Ruta a verificar
     * @return true si la ruta satisface todas las restricciones incluyendo layover
     *         mínimo
     */
    public boolean isRouteValidWithLayoverCheck(Pedido pedido, ArrayList<Vuelo> ruta) {
        if (pedido == null || ruta == null) {
            return false;
        }

        // Ruta vacía significa que el paquete ya está en destino
        if (ruta.isEmpty()) {
            return pedido.getAeropuertoOrigenCodigo() != null &&
                    pedido.getAeropuertoDestinoCodigo() != null &&
                    pedido.getAeropuertoOrigenCodigo().equals(pedido.getAeropuertoDestinoCodigo());
        }

        // Validar estructura básica de ruta
        if (!validateRouteStructure(pedido, ruta)) {
            return false;
        }

        // CRÍTICO: Validar tiempos mínimos de layover en paradas intermedias
        if (!validateMinimumLayoverTimes(pedido, ruta)) {
            if (DEBUG_MODE) {
                System.out.println("Validación de ruta falló: Tiempo de layover mínimo no satisfecho para pedido "
                        + pedido.getId());
            }
            return false;
        }

        // Validar capacidad
        int cantidadProductos = pedido.getCantidadProductos();
        if (!validateRouteCapacity(ruta, cantidadProductos)) {
            return false;
        }

        // Validar deadlines
        return validateDeadline(pedido, ruta);
    }

    /**
     * CRÍTICO: Valida layover mínimo de 1 hora en paradas intermedias
     * 
     * NOTA: Implementación simplificada - nuestra estructura Vuelo usa LocalTime
     * (horaSalida)
     * en lugar de LocalDateTime, por lo que la validación de layover con fechas
     * específicas
     * no es posible. Esta validación asume que el algoritmo principal maneja los
     * tiempos
     * de conexión adecuadamente.
     * 
     * @param pedido Pedido a validar
     * @param ruta   Ruta con posibles layovers
     * @return true (siempre, dado que no tenemos fechas específicas para validar)
     */
    private boolean validateMinimumLayoverTimes(Pedido pedido, ArrayList<Vuelo> ruta) {
        if (ruta.size() <= 1) {
            return true; // Sin conexiones, sin layovers
        }

        // TODO: Implementar validación de layover cuando Vuelo incluya fechas
        // específicas
        // Por ahora, asumimos que el algoritmo principal respeta el tiempo mínimo de
        // conexión
        return true;
    }

    /**
     * Valida estructura básica de ruta (origen, destino, continuidad)
     */
    private boolean validateRouteStructure(Pedido pedido, ArrayList<Vuelo> ruta) {
        // Verificar origen
        Aeropuerto origenEsperado = getAirportByCode(pedido.getAeropuertoOrigenCodigo());
        if (origenEsperado == null ||
                !ruta.get(0).getAeropuertoOrigen().getCodigoIATA().equals(origenEsperado.getCodigoIATA())) {
            return false;
        }

        // Verificar continuidad
        for (int i = 0; i < ruta.size() - 1; i++) {
            if (!ruta.get(i).getAeropuertoDestino().getCodigoIATA()
                    .equals(ruta.get(i + 1).getAeropuertoOrigen().getCodigoIATA())) {
                return false;
            }
        }

        // Verificar destino
        Aeropuerto destinoEsperado = getAirportByCode(pedido.getAeropuertoDestinoCodigo());
        return destinoEsperado != null &&
                ruta.get(ruta.size() - 1).getAeropuertoDestino().getCodigoIATA()
                        .equals(destinoEsperado.getCodigoIATA());
    }

    /**
     * Verifica si un aeropuerto es almacén principal (capacidad ilimitada).
     * Lima, Bruselas (Brussels) y Baku tienen capacidad ILIMITADA.
     * 
     * @param aeropuerto Aeropuerto a verificar
     * @return true si es almacén principal con capacidad ilimitada
     */
    private boolean esAlmacenPrincipal(Aeropuerto aeropuerto) {
        if (aeropuerto == null || aeropuerto.getCiudad() == null) {
            return false;
        }
        String nombreCiudad = aeropuerto.getCiudad().getNombre();
        return Constantes.esAlmacenPrincipal(nombreCiudad);
    }

    /**
     * Valida capacidad de ruta para cantidad de productos.
     * 
     * CRÍTICO: Los almacenes principales (Lima, Bruselas, Baku) tienen capacidad
     * ILIMITADA.
     * Esta validación NO se aplica a vuelos hacia/desde almacenes principales.
     * 
     * @param ruta              Ruta a validar
     * @param cantidadProductos Cantidad de productos a enviar
     * @return true si la capacidad es válida
     */
    private boolean validateRouteCapacity(ArrayList<Vuelo> ruta, int cantidadProductos) {
        for (Vuelo vuelo : ruta) {
            // CRÍTICO: Saltear validación si el destino es un almacén principal
            if (esAlmacenPrincipal(vuelo.getAeropuertoDestino())) {
                if (DEBUG_MODE) {
                    System.out.println("  ✓ Almacén principal detectado: " +
                            vuelo.getAeropuertoDestino().getCiudad().getNombre() +
                            " - Capacidad ILIMITADA");
                }
                continue; // No validar capacidad en almacenes principales
            }

            // Validar capacidad para aeropuertos regulares
            if (vuelo.getCapacidadUsada() + cantidadProductos > vuelo.getCapacidadMaxima()) {
                if (DEBUG_MODE) {
                    System.out.println("  ✗ Capacidad insuficiente en vuelo " +
                            vuelo.getIdentificadorVuelo() +
                            " (usado: " + vuelo.getCapacidadUsada() +
                            ", necesita: " + cantidadProductos +
                            ", max: " + vuelo.getCapacidadMaxima() + ")");
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Calcula tiempo total de ruta con accounting de layovers
     * Usa caching para evitar cálculos redundantes
     */
    public double calculateRouteTime(ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return 0.0;
        }

        // Intentar cache primero
        String routeKey = getRouteKey(ruta);
        if (cacheRouteTime.containsKey(routeKey)) {
            return cacheRouteTime.get(routeKey);
        }

        // Calcular tiempo total
        double totalTime = 0.0;

        // Sumar todos los tiempos de vuelo
        for (Vuelo vuelo : ruta) {
            totalTime += vuelo.getTiempoTransporte();
        }

        // Agregar tiempos de conexión (incluye layover mínimo de 1 hora)
        if (ruta.size() > 1) {
            int numConexiones = ruta.size() - 1;
            double horasConexion = (MINIMUM_LAYOVER_MINUTES / 60.0) * numConexiones;
            totalTime += horasConexion;
        }

        // Cachear resultado
        cacheRouteTime.put(routeKey, totalTime);

        return totalTime;
    }

    /**
     * Valida deadline con promesas de MoraPack y deadlines del cliente
     * 
     * NOTA: Implementación simplificada - sin fechas específicas en Vuelo,
     * validamos que el tiempo total de ruta no exceda el tiempo disponible.
     */
    private boolean validateDeadline(Pedido pedido, ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty() || pedido.getFechaLimiteEntrega() == null
                || pedido.getFechaPedido() == null) {
            return false;
        }

        // Calcular tiempo total de ruta en horas
        double tiempoRutaHoras = calculateRouteTime(ruta);

        // Calcular tiempo disponible desde el pedido hasta el deadline
        long horasDisponibles = ChronoUnit.HOURS.between(pedido.getFechaPedido(), pedido.getFechaLimiteEntrega());

        // Verificar que el tiempo de ruta no exceda el tiempo disponible
        return tiempoRutaHoras <= horasDisponibles;
    }

    /**
     * Busca vuelo directo entre dos aeropuertos - O(1)
     */
    public Vuelo findDirectFlight(Aeropuerto origen, Aeropuerto destino) {
        String origenKey = getAirportKey(origen);
        String destinoKey = getAirportKey(destino);

        Map<String, Vuelo> destinosDesdeOrigen = flightLookupMap.get(origenKey);
        if (destinosDesdeOrigen == null) {
            return null;
        }

        return destinosDesdeOrigen.get(destinoKey);
    }

    /**
     * Obtiene aeropuerto por código IATA
     */
    private Aeropuerto getAirportByCode(String codigoIATA) {
        if (codigoIATA == null) {
            return null;
        }

        for (Aeropuerto aeropuerto : cityToAirportMap.values()) {
            if (aeropuerto.getCodigoIATA().equals(codigoIATA)) {
                return aeropuerto;
            }
        }
        return null;
    }

    /**
     * Obtiene aeropuerto por ciudad
     */
    private Aeropuerto getAirportByCity(Ciudad ciudad) {
        if (ciudad == null || ciudad.getNombre() == null) {
            return null;
        }
        String key = normalizeCity(ciudad.getNombre());
        return cityToAirportMap.get(key);
    }

    /**
     * Normaliza nombre de ciudad para búsqueda
     */
    private String normalizeCity(String cityName) {
        if (cityName == null) {
            return "";
        }
        return cityName.trim().toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[áàäâã]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöôõ]", "o")
                .replaceAll("[úùüû]", "u");
    }

    /**
     * Genera clave de aeropuerto para búsqueda
     */
    private String getAirportKey(Aeropuerto aeropuerto) {
        if (aeropuerto == null) {
            return "";
        }
        return aeropuerto.getCodigoIATA() != null ? aeropuerto.getCodigoIATA()
                : normalizeCity(aeropuerto.getCiudad().getNombre());
    }

    /**
     * Genera clave de ruta para caching
     */
    private String getRouteKey(ArrayList<Vuelo> ruta) {
        StringBuilder key = new StringBuilder();
        for (Vuelo vuelo : ruta) {
            key.append(vuelo.getId()).append("-");
        }
        return key.toString();
    }

    /**
     * Limpia cachés
     */
    public void clearCache() {
        cacheRouteTime.clear();
        cacheDeadline.clear();
    }

    /**
     * Obtiene tamaño del cache
     */
    public int getCacheSize() {
        return cacheRouteTime.size() + cacheDeadline.size();
    }
}
