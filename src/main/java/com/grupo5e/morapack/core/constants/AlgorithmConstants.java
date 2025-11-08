package com.grupo5e.morapack.core.constants;

/**
 * AlgorithmConstants - Constantes del algoritmo ALNS
 * Siguiendo patrón de MoraPack-Backend/Constants.java
 * 
 * Centraliza todos los parámetros configurables del algoritmo,
 * permitiendo tuning sin modificar el código del solver.
 */
public class AlgorithmConstants {
    
    // ========== ALNS DESTRUCTION PARAMETERS ==========
    /**
     * Ratio de destrucción para ALNS (15% recomendado)
     */
    public static final double DESTRUCTION_RATIO = 0.15;
    
    /**
     * Mínimo número de pedidos a destruir
     */
    public static final int DESTRUCTION_MIN_PACKAGES = 10;
    
    /**
     * Máximo número de pedidos a destruir
     */
    public static final int DESTRUCTION_MAX_PACKAGES = 500;
    
    /**
     * Para expansiones más controladas
     */
    public static final int DESTRUCTION_MAX_PACKAGES_EXPANSION = 100;
    
    // ========== DELIVERY TIME CONSTRAINTS ==========
    /**
     * Tiempo máximo de entrega mismo continente (días)
     */
    public static final double SAME_CONTINENT_MAX_DELIVERY_TIME = 2.0;
    
    /**
     * Tiempo máximo de entrega diferente continente (días)
     */
    public static final double DIFFERENT_CONTINENT_MAX_DELIVERY_TIME = 3.0;
    
    /**
     * Tiempo de transporte mismo continente (días)
     */
    public static final double SAME_CONTINENT_TRANSPORT_TIME = 0.5;
    
    /**
     * Tiempo de transporte diferente continente (días)
     */
    public static final double DIFFERENT_CONTINENT_TRANSPORT_TIME = 1.0;
    
    // ========== CAPACITY CONSTRAINTS ==========
    /**
     * Capacidad mínima de vuelos mismo continente
     */
    public static final int SAME_CONTINENT_MIN_CAPACITY = 200;
    
    /**
     * Capacidad máxima de vuelos mismo continente
     */
    public static final int SAME_CONTINENT_MAX_CAPACITY = 300;
    
    /**
     * Capacidad mínima de vuelos diferente continente
     */
    public static final int DIFFERENT_CONTINENT_MIN_CAPACITY = 250;
    
    /**
     * Capacidad máxima de vuelos diferente continente
     */
    public static final int DIFFERENT_CONTINENT_MAX_CAPACITY = 400;
    
    /**
     * Capacidad mínima de almacenes
     */
    public static final int WAREHOUSE_MIN_CAPACITY = 600;
    
    /**
     * Capacidad máxima de almacenes
     */
    public static final int WAREHOUSE_MAX_CAPACITY = 1000;
    
    /**
     * Tiempo máximo para pickup del cliente en aeropuerto destino (horas)
     */
    public static final int CUSTOMER_PICKUP_MAX_HOURS = 2;
    
    // ========== INITIAL SOLUTION CONTROL ==========
    /**
     * Tipo de solución inicial: true=greedy, false=random
     */
    public static final boolean USE_GREEDY_INITIAL_SOLUTION = true;
    
    /**
     * Probabilidad de asignación para solución inicial random (30%)
     */
    public static final double RANDOM_ASSIGNMENT_PROBABILITY = 0.3;
    
    // ========== LOGGING CONTROL ==========
    /**
     * Activar logs detallados del algoritmo
     */
    public static final boolean VERBOSE_LOGGING = false;
    
    /**
     * Mostrar logs solo cada X iteraciones
     */
    public static final int LOG_ITERATION_INTERVAL = 100;
    
    // ========== DIVERSIFICATION & RESTART ==========
    /**
     * Iteraciones sin mejora significativa para activar restart
     */
    public static final int STAGNATION_THRESHOLD_FOR_RESTART = 50;
    
    /**
     * Porcentaje mínimo para considerar mejora significativa (0.1%)
     */
    public static final double SIGNIFICANT_IMPROVEMENT_THRESHOLD = 0.1;
    
    /**
     * Ratio de destrucción extrema para restart (80%)
     */
    public static final double EXTREME_DESTRUCTION_RATIO = 0.8;
    
    /**
     * Máximo número de restarts por ejecución
     */
    public static final int MAX_RESTARTS = 3;
    
    // ========== MORAPACK HEADQUARTERS ==========
    /**
     * Sede principal: Lima, Perú
     */
    public static final String LIMA_WAREHOUSE = "Lima, Peru";
    
    /**
     * Sede principal: Bruselas, Bélgica
     */
    public static final String BRUSSELS_WAREHOUSE = "Brussels, Belgium";
    
    /**
     * Sede principal: Bakú, Azerbaiyán
     */
    public static final String BAKU_WAREHOUSE = "Baku, Azerbaijan";
    
    // ========== PRODUCT UNITIZATION ==========
    /**
     * CRÍTICO: Activar unitización de productos
     * true = Cada producto viaja independiente (permite división de pedidos)
     * false = Pedidos viajan completos (sin división)
     */
    public static final boolean ENABLE_PRODUCT_UNITIZATION = true;
    
    // ========== TEMPORAL VALIDATION ==========
    /**
     * Horizonte temporal en días para planificación
     */
    public static final int HORIZON_DAYS = 4;
    
    /**
     * CRÍTICO: Tiempo mínimo de layover en destinos intermedios (minutos)
     * "Los tiempos de estancia mínima para los productos en tránsito es de 1 hora"
     */
    public static final int MIN_LAYOVER_TIME_MINUTES = 60;
    
    /**
     * Tiempo de conexión entre vuelos (minutos) - incluye layover mínimo
     */
    public static final int CONNECTION_TIME_MINUTES = 120;
    
    /**
     * Tiempo de procesamiento prevuelo (minutos)
     */
    public static final int PRE_FLIGHT_PROCESSING_MINUTES = 120;
    
    // ========== HEADQUARTERS VALIDATION ==========
    /**
     * Validar que los pedidos se originen desde sedes principales
     * (Lima, Brussels, Baku)
     */
    public static final boolean VALIDATE_HEADQUARTERS_ORIGIN = true;
    
    // ========== DATA SOURCE CONFIGURATION ==========
    /**
     * Modo de fuente de datos para el algoritmo
     * - DATABASE: Lee desde BD (modo actual)
     * - FILE: Lee desde archivos data/ (modo legacy)
     */
    public static final DataSourceMode DATA_SOURCE_MODE = DataSourceMode.DATABASE;
    
    /**
     * Enum para selección de fuente de datos
     */
    public enum DataSourceMode {
        /**
         * Lee desde archivos en directorio data/
         */
        FILE,
        
        /**
         * Lee desde base de datos PostgreSQL vía JPA
         */
        DATABASE
    }
    
    // ========== SOLUTION SPACE BOUNDS ==========
    /**
     * Límite inferior del espacio de soluciones
     */
    public static final int LOWERBOUND_SOLUTION_SPACE = 100;
    
    /**
     * Límite superior del espacio de soluciones
     */
    public static final int UPPERBOUND_SOLUTION_SPACE = 200;
    
    // ========== PRIVATE CONSTRUCTOR ==========
    /**
     * Constructor privado para prevenir instanciación
     */
    private AlgorithmConstants() {
        throw new UnsupportedOperationException("Esta es una clase de utilidad y no debe ser instanciada");
    }
}

