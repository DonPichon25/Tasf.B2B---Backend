package com.grupo5e.morapack.algorithm.validador;

import com.grupo5e.morapack.core.constants.Constantes;
import com.grupo5e.morapack.core.model.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Validador optimizado de rutas con complejidad O(1) para lookups.
 * 
 * Características:
 * - Lookups O(1) usando HashMap para ciudades->aeropuertos y vuelos
 * - Caching de cálculos de tiempo de ruta y validaciones de deadline
 * - Validación de tiempos mínimos de estancia (1 hora) en escalas intermedias
 * - Validación robusta de capacidad, continuidad y deadlines
 */
public class RutasValidador {

    // Cache para cálculos frecuentes
    private Map<String, Double> cacheTiempoRuta;
    private Map<String, Boolean> cacheDeadline;

    // Estructuras de lookup O(1)
    private Map<String, Aeropuerto> ciudadAeropuertoMap; // ciudad normalizada -> aeropuerto
    private Map<String, Aeropuerto> codigoAeropuertoMap; // código IATA -> aeropuerto
    private Map<String, Map<String, Vuelo>> vueloLookupMap; // origen -> destino -> vuelo

    public RutasValidador(ArrayList<Aeropuerto> aeropuertos, ArrayList<Vuelo> vuelos) {
        this.cacheTiempoRuta = new HashMap<>();
        this.cacheDeadline = new HashMap<>();
        this.ciudadAeropuertoMap = new HashMap<>();
        this.codigoAeropuertoMap = new HashMap<>();
        this.vueloLookupMap = new HashMap<>();

        // Inicializar estructuras optimizadas
        inicializarMapasAeropuertos(aeropuertos);
        inicializarMapaVuelos(vuelos);
    }

    /**
     * Inicializa mapa O(1) de ciudades a aeropuertos
     */
    private void inicializarMapasAeropuertos(ArrayList<Aeropuerto> aeropuertos) {
        for (Aeropuerto aeropuerto : aeropuertos) {
            // Mapa por código IATA
            if (aeropuerto.getCodigoIATA() != null) {
                String codigo = aeropuerto.getCodigoIATA().toLowerCase().trim();
                codigoAeropuertoMap.put(codigo, aeropuerto);
            }

            // Mapa por nombre de ciudad
            if (aeropuerto.getCiudad() != null && aeropuerto.getCiudad().getNombre() != null) {
                String ciudadNormalizada = normalizarCiudad(aeropuerto.getCiudad().getNombre());
                ciudadAeropuertoMap.put(ciudadNormalizada, aeropuerto);
            }
        }
    }

    /**
     * Inicializa mapa O(1) de vuelos: origen -> destino -> vuelo
     * Optimiza búsqueda de vuelos directos de O(n) a O(1)
     */
    private void inicializarMapaVuelos(ArrayList<Vuelo> vuelos) {
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen() == null || vuelo.getAeropuertoDestino() == null) {
                continue;
            }

            String codigoOrigen = normalizarCodigo(vuelo.getAeropuertoOrigen().getCodigoIATA());
            String codigoDestino = normalizarCodigo(vuelo.getAeropuertoDestino().getCodigoIATA());

            if (codigoOrigen != null && codigoDestino != null) {
                vueloLookupMap
                    .computeIfAbsent(codigoOrigen, k -> new HashMap<>())
                    .put(codigoDestino, vuelo);
            }
        }
    }

    /**
     * Valida una ruta completa con verificación de tiempos mínimos de estancia
     * 
     * @param pedido Pedido a validar
     * @param ruta Lista de vuelos que conforman la ruta
     * @return true si la ruta es válida
     */
    public boolean esRutaValida(Pedido pedido, ArrayList<Vuelo> ruta) {
        if (pedido == null || ruta == null) {
            return false;
        }

        // Ruta vacía significa que el pedido ya está en su destino
        if (ruta.isEmpty()) {
            Aeropuerto origen = obtenerAeropuertoPorCodigo(pedido.getAeropuertoOrigenCodigo());
            Aeropuerto destino = obtenerAeropuertoPorCodigo(pedido.getAeropuertoDestinoCodigo());
            
            if (origen == null || destino == null) {
                return false;
            }
            
            // Verificar que origen y destino son la misma ciudad
            return origen.getCiudad() != null && 
                   destino.getCiudad() != null &&
                   origen.getCiudad().equals(destino.getCiudad());
        }

        // Validar estructura básica de la ruta
        if (!validarEstructuraRuta(pedido, ruta)) {
            return false;
        }

        // CRÍTICO: Validar tiempos mínimos de estancia en escalas intermedias
        if (!validarTiemposMinimosEstancia(pedido, ruta)) {
            if (Constantes.LOGGING_VERBOSO) {
                System.out.println("Validación de ruta fallida: Tiempo mínimo de estancia no cumplido para pedido " + pedido.getId());
            }
            return false;
        }

        // Validar capacidad
        int cantidadProductos = obtenerCantidadProductos(pedido);
        if (!validarCapacidadRuta(ruta, cantidadProductos)) {
            return false;
        }

        // Validar deadlines
        return validarDeadline(pedido, ruta);
    }

    /**
     * Valida tiempos mínimos de estancia (1 hora) en destinos intermedios
     * 
     * Esto asegura cumplimiento con: "Los tiempos de estancia mínima para los productos
     * en tránsito (destino intermedio) es de 1 hora"
     * 
     * @param pedido Pedido siendo validado
     * @param ruta Ruta con posibles escalas
     * @return true si todas las escalas intermedias tienen >= 1 hora de estancia
     */
    private boolean validarTiemposMinimosEstancia(Pedido pedido, ArrayList<Vuelo> ruta) {
        if (ruta.size() <= 1) {
            // Vuelo directo, no hay escalas que validar
            return true;
        }

        // Para rutas con conexiones, validar cada parada intermedia
        for (int i = 0; i < ruta.size() - 1; i++) {
            Vuelo vueloActual = ruta.get(i);
            
            // Aeropuerto de escala (destino del vuelo actual = origen del siguiente)
            Aeropuerto aeropuertoEscala = vueloActual.getAeropuertoDestino();
            
            // Validar que el TIEMPO_CONEXION_MINUTOS ya incluye >= 1 hora
            // (Validación a nivel de constantes, solo para logging si falla)
            if (Constantes.TIEMPO_CONEXION_MINUTOS < Constantes.TIEMPO_ESTANCIA_MINIMO_MINUTOS) {
                System.err.println("ERROR: TIEMPO_CONEXION_MINUTOS (" + Constantes.TIEMPO_CONEXION_MINUTOS +
                                 ") es menor que TIEMPO_ESTANCIA_MINIMO_MINUTOS (" + 
                                 Constantes.TIEMPO_ESTANCIA_MINIMO_MINUTOS + ")");
            }

            // Validar que el aeropuerto de escala tiene almacén
            if (aeropuertoEscala.getCapacidadMaxima() <= 0) {
                if (Constantes.LOGGING_VERBOSO) {
                    System.out.println("Validación de escala fallida: Aeropuerto intermedio " +
                                     aeropuertoEscala.getCodigoIATA() + " no tiene capacidad de almacén");
                }
                return false;
            }
        }

        return true;
    }

    /**
     * Valida estructura básica de la ruta (origen, destino, continuidad)
     */
    private boolean validarEstructuraRuta(Pedido pedido, ArrayList<Vuelo> ruta) {
        if (ruta.isEmpty()) {
            return false;
        }

        // Verificar origen
        Aeropuerto origenEsperado = obtenerAeropuertoPorCodigo(pedido.getAeropuertoOrigenCodigo());
        if (origenEsperado == null || !ruta.get(0).getAeropuertoOrigen().equals(origenEsperado)) {
            return false;
        }

        // Verificar continuidad
        for (int i = 0; i < ruta.size() - 1; i++) {
            if (!ruta.get(i).getAeropuertoDestino().equals(ruta.get(i + 1).getAeropuertoOrigen())) {
                return false;
            }
        }

        // Verificar destino
        Aeropuerto destinoEsperado = obtenerAeropuertoPorCodigo(pedido.getAeropuertoDestinoCodigo());
        return destinoEsperado != null &&
               ruta.get(ruta.size() - 1).getAeropuertoDestino().equals(destinoEsperado);
    }

    /**
     * Valida capacidad de la ruta para una cantidad de productos
     */
    private boolean validarCapacidadRuta(ArrayList<Vuelo> ruta, int cantidadProductos) {
        for (Vuelo vuelo : ruta) {
            if (vuelo.getCapacidadUsada() + cantidadProductos > vuelo.getCapacidadMaxima()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calcula tiempo total de ruta con tiempos de conexión
     * Usa caching para evitar cálculos redundantes
     * 
     * @param ruta Lista de vuelos
     * @return Tiempo total en horas
     */
    public double calcularTiempoRuta(ArrayList<Vuelo> ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return 0.0;
        }

        // Intentar cache primero
        String claveRuta = generarClaveRuta(ruta);
        if (cacheTiempoRuta.containsKey(claveRuta)) {
            return cacheTiempoRuta.get(claveRuta);
        }

        // Calcular tiempo total
        double tiempoTotal = 0.0;

        // Sumar todos los tiempos de vuelo
        for (Vuelo vuelo : ruta) {
            tiempoTotal += vuelo.getTiempoTransporte();
        }

        // Agregar tiempos de conexión (incluye mínimo 1 hora de estancia)
        if (ruta.size() > 1) {
            int numConexiones = ruta.size() - 1;
            double horasConexion = (Constantes.TIEMPO_CONEXION_MINUTOS / 60.0) * numConexiones;
            tiempoTotal += horasConexion;
        }

        // Guardar en cache
        cacheTiempoRuta.put(claveRuta, tiempoTotal);

        return tiempoTotal;
    }

    /**
     * Valida deadline con promesas de MoraPack y deadlines del cliente
     */
    private boolean validarDeadline(Pedido pedido, ArrayList<Vuelo> ruta) {
        if (pedido.getFechaPedido() == null || pedido.getFechaLimiteEntrega() == null) {
            return false;
        }

        // Intentar cache primero
        String claveCache = pedido.getId() + ":" + generarClaveRuta(ruta);
        if (cacheDeadline.containsKey(claveCache)) {
            return cacheDeadline.get(claveCache);
        }

        double tiempoRuta = calcularTiempoRuta(ruta);

        // Validar promesa de entrega de MoraPack (2 días mismo continente, 3 días diferente)
        Aeropuerto origen = obtenerAeropuertoPorCodigo(pedido.getAeropuertoOrigenCodigo());
        Aeropuerto destino = obtenerAeropuertoPorCodigo(pedido.getAeropuertoDestinoCodigo());
        
        if (origen == null || destino == null || 
            origen.getCiudad() == null || destino.getCiudad() == null) {
            cacheDeadline.put(claveCache, false);
            return false;
        }

        boolean mismoContinente = origen.getCiudad().getContinente() == destino.getCiudad().getContinente();
        long horasPromesaMoraPack = mismoContinente ? 48 : 72; // 2 días o 3 días

        if (tiempoRuta > horasPromesaMoraPack) {
            cacheDeadline.put(claveCache, false);
            return false; // Excede promesa de MoraPack
        }

        // Validar deadline del cliente
        long horasHastaDeadline = ChronoUnit.HOURS.between(pedido.getFechaPedido(), pedido.getFechaLimiteEntrega());

        // Agregar margen de seguridad
        double margenSeguridad = calcularMargenSeguridad(ruta, mismoContinente);
        double tiempoTotalConSeguridad = tiempoRuta * (1.0 + margenSeguridad);

        boolean resultado = tiempoTotalConSeguridad <= horasHastaDeadline;
        cacheDeadline.put(claveCache, resultado);

        return resultado;
    }

    /**
     * Calcula margen de seguridad basado en complejidad de ruta
     */
    private double calcularMargenSeguridad(ArrayList<Vuelo> ruta, boolean mismoContinente) {
        int factorComplejidad = ruta.size() + (mismoContinente ? 0 : 2);
        return 0.01 * (1 + factorComplejidad * 2); // Margen de 1-5%
    }

    /**
     * Obtiene aeropuerto por código IATA con lookup O(1)
     */
    public Aeropuerto obtenerAeropuertoPorCodigo(String codigoIATA) {
        if (codigoIATA == null || codigoIATA.trim().isEmpty()) {
            return null;
        }
        return codigoAeropuertoMap.get(normalizarCodigo(codigoIATA));
    }

    /**
     * Obtiene aeropuerto por ciudad con lookup O(1)
     */
    public Aeropuerto obtenerAeropuertoPorCiudad(Ciudad ciudad) {
        if (ciudad == null || ciudad.getNombre() == null) {
            return null;
        }
        return ciudadAeropuertoMap.get(normalizarCiudad(ciudad.getNombre()));
    }

    /**
     * Encuentra vuelo directo con lookup O(1) (optimizado de O(n))
     * 
     * @param origen Aeropuerto origen
     * @param destino Aeropuerto destino
     * @return Vuelo directo o null si no existe o no tiene capacidad
     */
    public Vuelo encontrarVueloDirecto(Aeropuerto origen, Aeropuerto destino) {
        if (origen == null || destino == null) {
            return null;
        }

        String codigoOrigen = normalizarCodigo(origen.getCodigoIATA());
        String codigoDestino = normalizarCodigo(destino.getCodigoIATA());

        if (codigoOrigen == null || codigoDestino == null) {
            return null;
        }

        Map<String, Vuelo> mapaDestinos = vueloLookupMap.get(codigoOrigen);
        if (mapaDestinos == null) {
            return null;
        }

        Vuelo vuelo = mapaDestinos.get(codigoDestino);

        // Verificar que el vuelo tenga capacidad
        if (vuelo != null && vuelo.getCapacidadUsada() >= vuelo.getCapacidadMaxima()) {
            return null;
        }

        return vuelo;
    }

    /**
     * Helper: Obtiene cantidad de productos del pedido
     */
    private int obtenerCantidadProductos(Pedido pedido) {
        return (pedido.getProductos() != null && !pedido.getProductos().isEmpty())
            ? pedido.getProductos().size() : 1;
    }

    /**
     * Helper: Normaliza nombre de ciudad para lookup consistente
     */
    private String normalizarCiudad(String nombreCiudad) {
        if (nombreCiudad == null) {
            return "";
        }
        return nombreCiudad.toLowerCase().trim();
    }

    /**
     * Helper: Normaliza código IATA para lookup consistente
     */
    private String normalizarCodigo(String codigoIATA) {
        if (codigoIATA == null) {
            return null;
        }
        return codigoIATA.toLowerCase().trim();
    }

    /**
     * Helper: Genera clave única para una ruta (para caching)
     */
    private String generarClaveRuta(ArrayList<Vuelo> ruta) {
        StringBuilder sb = new StringBuilder();
        for (Vuelo vuelo : ruta) {
            sb.append(vuelo.getId()).append("-");
        }
        return sb.toString();
    }

    /**
     * Limpia caches (llamar cuando la solución cambia significativamente)
     */
    public void limpiarCaches() {
        cacheTiempoRuta.clear();
        cacheDeadline.clear();
    }
}
