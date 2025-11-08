package com.grupo5e.morapack.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger especializado para simulaciones que guarda logs en archivos
 * para evitar que se pierdan en la consola
 */
public class SimulationLogger {
    
    private static final String LOGS_DIR = "simulation-logs";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String logFilePath;
    private final Long simulationId;
    
    public SimulationLogger(Long simulationId) {
        this.simulationId = simulationId;
        
        // Crear directorio de logs si no existe
        File logsDir = new File(LOGS_DIR);
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        
        // Crear archivo de log específico para esta simulación
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        this.logFilePath = LOGS_DIR + "/simulation_" + simulationId + "_" + timestamp + ".log";
        
        // Escribir encabezado
        log("╔════════════════════════════════════════════════════════════════╗");
        log("║          MORAPACK - LOG DE SIMULACIÓN #" + simulationId + "                 ║");
        log("║          " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "                                  ║");
        log("╚════════════════════════════════════════════════════════════════╝");
        log("");
    }
    
    /**
     * Registra un mensaje en el archivo de log
     */
    public void log(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            writer.write("[" + timestamp + "] " + message);
            writer.newLine();
            
            // También imprimir en consola
            System.out.println(message);
        } catch (IOException e) {
            System.err.println("❌ Error escribiendo log: " + e.getMessage());
        }
    }
    
    /**
     * Registra un error en el archivo de log
     */
    public void error(String message) {
        log("❌ ERROR: " + message);
    }
    
    /**
     * Registra una advertencia en el archivo de log
     */
    public void warning(String message) {
        log("⚠️ WARNING: " + message);
    }
    
    /**
     * Registra información en el archivo de log
     */
    public void info(String message) {
        log("ℹ️ INFO: " + message);
    }
    
    /**
     * Registra éxito en el archivo de log
     */
    public void success(String message) {
        log("✅ SUCCESS: " + message);
    }
    
    /**
     * Registra una sección separada
     */
    public void section(String title) {
        log("");
        log("═══════════════════════════════════════════════════════════════");
        log("  " + title);
        log("═══════════════════════════════════════════════════════════════");
    }
    
    /**
     * Registra estadísticas en formato tabla
     */
    public void stats(String title, Object... keyValues) {
        log("");
        log("📊 " + title);
        log("─────────────────────────────────────────");
        
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i + 1 < keyValues.length) {
                String key = String.valueOf(keyValues[i]);
                String value = String.valueOf(keyValues[i + 1]);
                log(String.format("   %-30s : %s", key, value));
            }
        }
        log("─────────────────────────────────────────");
    }
    
    /**
     * Cierra el log con un resumen final
     */
    public void close() {
        log("");
        log("╔════════════════════════════════════════════════════════════════╗");
        log("║          LOG FINALIZADO - " + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "                ║");
        log("╚════════════════════════════════════════════════════════════════╝");
        log("");
        log("📁 Log guardado en: " + logFilePath);
    }
    
    /**
     * Obtiene la ruta del archivo de log
     */
    public String getLogFilePath() {
        return logFilePath;
    }
}

