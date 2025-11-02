package com.grupo5e.morapack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de validación para archivos de simulación
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadValidationResponse {
    
    /**
     * ID de sesión único para identificar los datos temporales
     * Solo se genera si al menos un archivo fue validado exitosamente
     */
    private String sessionId;
    
    /**
     * Indica si la validación general fue exitosa
     * (al menos un archivo válido o todos los archivos opcionales omitidos)
     */
    private boolean success;
    
    /**
     * Resultado de validación para aeropuertos.txt
     */
    private FileValidationResult aeropuertos;
    
    /**
     * Resultado de validación para vuelos.txt
     */
    private FileValidationResult vuelos;
    
    /**
     * Resultado de validación para pedidos.txt
     */
    private FileValidationResult pedidos;
    
    /**
     * Mensaje general sobre el resultado
     */
    private String message;
    
    /**
     * Indica si se usarán datos de la base de datos como fallback
     */
    private boolean usingDatabaseFallback;
}

