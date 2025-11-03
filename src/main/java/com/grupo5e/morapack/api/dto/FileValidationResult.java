package com.grupo5e.morapack.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Vuelo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de validación para un archivo individual
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileValidationResult {
    
    /**
     * Indica si el archivo pasó la validación
     */
    private boolean success;
    
    /**
     * Tipo de archivo validado
     */
    private String fileType;
    
    /**
     * Mensajes de error (si los hay)
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    
    /**
     * Mensajes de advertencia (si los hay)
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * Cantidad de registros parseados exitosamente
     */
    private Integer parsedCount;
    
    /**
     * Información adicional sobre el archivo
     */
    private String info;
    
    /**
     * Datos parseados (se ignoran en JSON pero se usan internamente)
     */
    @JsonIgnore
    private transient List<Aeropuerto> parsedAeropuertos;
    
    @JsonIgnore
    private transient List<Vuelo> parsedVuelos;
    
    @JsonIgnore
    private transient List<Pedido> parsedPedidos;
    
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.success = false;
    }
    
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
}

