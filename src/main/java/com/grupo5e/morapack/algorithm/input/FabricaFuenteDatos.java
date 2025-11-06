package com.grupo5e.morapack.algorithm.input;

import org.springframework.context.ApplicationContext;

/**
 * Fábrica para crear instancias de FuenteDatosInput según el modo configurado.
 * 
 * Permite cambiar entre modo ARCHIVO y BASEDATOS sin modificar el algoritmo.
 * El modo se configura mediante variable de entorno MODO_FUENTE_DATOS.
 * 
 * Uso:
 * <pre>
 * // Sin contexto Spring (solo ARCHIVO)
 * FuenteDatosInput fuente = FabricaFuenteDatos.crearFuenteDatos();
 * 
 * // Con contexto Spring (soporta BASEDATOS)
 * FuenteDatosInput fuente = FabricaFuenteDatos.crearFuenteDatos(context);
 * </pre>
 */
public class FabricaFuenteDatos {
    
    private static ApplicationContext springContext;
    
    /**
     * Establece el contexto de Spring para soporte de modo BASEDATOS.
     * Debe llamarse durante la inicialización de la aplicación.
     * 
     * @param context Contexto de Spring
     */
    public static void setSpringContext(ApplicationContext context) {
        springContext = context;
    }
    
    /**
     * Crea una instancia de FuenteDatosInput según el modo configurado.
     * 
     * Por defecto usa ARCHIVO si no se especifica modo o si Spring no está disponible.
     * 
     * @return FuenteDatosInput (FuenteDatosArchivo o FuenteDatosBaseDatos)
     */
    public static FuenteDatosInput crearFuenteDatos() {
        // ✅ Usar System.getProperty() en lugar de System.getenv()
        // porque AlgoritmoController usa System.setProperty()
        String modo = System.getProperty("MODO_FUENTE_DATOS");
        if (modo == null) {
            modo = "ARCHIVO"; // Modo por defecto
        }
        
        System.out.println("[FABRICA] Creando fuente de datos para modo: " + modo);
        
        if ("BASEDATOS".equals(modo) || "BASE_DE_DATOS".equals(modo)) {
            if (springContext != null) {
                System.out.println("[FABRICA] ✅ Usando fuente de datos BASEDATOS (H2/PostgreSQL)");
                return springContext.getBean(FuenteDatosBaseDatos.class);
            } else {
                System.err.println("[FABRICA] ⚠️ ADVERTENCIA: Modo BASEDATOS seleccionado pero Spring no disponible");
                System.err.println("[FABRICA] Recurriendo a modo ARCHIVO");
                return new FuenteDatosArchivo();
            }
        }
        
        // Modo ARCHIVO (por defecto)
        System.out.println("[FABRICA] Usando fuente de datos ARCHIVO (data/*.txt)");
        return new FuenteDatosArchivo();
    }
    
    /**
     * Crea una instancia con contexto Spring explícito.
     * Útil cuando se llama desde componentes gestionados por Spring.
     * 
     * @param context Contexto de Spring
     * @return FuenteDatosInput
     */
    public static FuenteDatosInput crearFuenteDatos(ApplicationContext context) {
        setSpringContext(context);
        return crearFuenteDatos();
    }
    
    /**
     * Verifica si el modo BASEDATOS está disponible.
     * 
     * @return true si Spring está configurado y modo BASEDATOS puede usarse
     */
    public static boolean esModoBaseDatosDisponible() {
        return springContext != null;
    }
    
    /**
     * Limpia la referencia al contexto de Spring.
     * Útil para testing o reinicio del estado de la fábrica.
     */
    public static void limpiarSpringContext() {
        springContext = null;
    }
}

