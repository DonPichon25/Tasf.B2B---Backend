package com.grupo5e.morapack.tabusearch.config;

/**
 * Parametros generales de la ejecucion TabuSearch y del planificador.
 *
 * Se expone como clase con valores por defecto (ajustables antes de correr).
 * NO se lee de archivo; si el usuario quiere cambiar estos valores, los edita
 * aqui o los setea desde Main antes de ejecutar.
 *
 * Unidades de tiempo: MINUTOS.
 */
public class ConfiguracionTabu {

    // --------------------------- PLAZOS ---------------------------

    /**
     * Plazo maximo para envios dentro del mismo continente (24 horas).
     * Coincide con la definicion oficial del caso PDDS 2026-1.
     */
    public int plazoMismoContinenteMin = 24 * 60;        // 1440 min

    /**
     * Plazo maximo para envios entre continentes distintos (48 horas).
     */
    public int plazoDistintoContinenteMin = 48 * 60;     // 2880 min

    // ---------------------- TIEMPOS OPERATIVOS --------------------

    /**
     * Minutos minimos entre la llegada de un vuelo y la salida del siguiente
     * en la misma ruta (carga/descarga en escalas). Regla: 10 minutos.
     */
    public int tiempoEscalaMin = 10;

    /**
     * Minutos que una maleta ocupa almacen en el aeropuerto destino despues
     * de aterrizar, antes de ser liberada al cliente. Tambien 10 minutos.
     * Se suma al tiempo de entrega y a la ocupacion del almacen destino.
     */
    public int tiempoLiberacionDestinoMin = 10;

    // ------------------------ CARGA DE DATOS ----------------------

    /**
     * Limite de envios a leer por archivo _envios_XXXX_.txt.
     * Los archivos reales tienen ~300K lineas cada uno. Para pruebas se usa
     * un limite razonable.
     */
    public int maxEnviosPorArchivo = 200;

    /** Limite global de envios cargados en toda la corrida. */
    public int limiteGlobalEnvios = 60_000;

    // ------------------------ TABU SEARCH -------------------------

    /** Numero maximo de iteraciones del TabuSearch. */
    public int maxIteracionesTabu = 60;

    /** Tamano de la lista tabu (cantidad de movimientos recientes prohibidos). */
    public int tamanoListaTabu = 20;

    /**
     * Cantidad de rutas alternativas a generar por envio al construir un
     * vecindario (candidatas para "cambio de ruta"). Valores mayores mejoran
     * calidad pero encarecen cada iteracion.
     */
    public int candidatasPorEnvio = 5;

    /**
     * Maxima profundidad (numero de tramos) de una ruta. Con 30 aeropuertos
     * rara vez se necesitan mas de 3 o 4 tramos.
     */
    public int maxTramosPorRuta = 4;

    /** Tiempo maximo del TabuSearch en milisegundos (seguridad). */
    public long tiempoMaxTabuMs = 120_000L; // 2 minutos

    /** Semilla para reproducibilidad del componente aleatorio. */
    public long semilla = 42L;

    // ----------------- PESOS DE LA FUNCION OBJETIVO ---------------

    /** Penalidad por cada envio sin ruta asignada. Grande para priorizarlos. */
    public double pesoSinRuta = 1_000_000.0;

    /** Penalidad por cada envio que llega fuera de plazo. */
    public double pesoEnvioTarde = 10_000.0;

    /** Penalidad por minuto total de retraso (cuando llega tarde). */
    public double pesoMinutoRetraso = 1.0;

    /** Penalidad por minuto total de "tiempo en red" (mantiene rutas cortas). */
    public double pesoMinutoEnRed = 0.01;

    // -------------------- SALIDAS DEL PLANIFICADOR ----------------

    /**
     * Ruta (relativa) al directorio Caso/DATA del caso TASF-B2B.
     * Main los utiliza por defecto si no se pasan argumentos en CLI.
     */
    public String dirDatos = "Caso/DATA";

    /** Archivo de reporte de rutas generado al terminar la corrida. */
    public String archivoReporte = "reporte_rutas_tabusearch.txt";

    public ConfiguracionTabu() { }
}
