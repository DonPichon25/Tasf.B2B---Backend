package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Vuelo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Clase que contiene datos temporales parseados de archivos subidos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporarySimulationData {
    
    /**
     * ID de sesión único
     */
    private String sessionId;
    
    /**
     * Fecha y hora de creación
     */
    private LocalDateTime createdAt;
    
    /**
     * Lista de aeropuertos parseados (null si no se subió archivo)
     */
    private List<Aeropuerto> aeropuertos;
    
    /**
     * Lista de vuelos parseados (null si no se subió archivo)
     */
    private List<Vuelo> vuelos;
    
    /**
     * Lista de pedidos parseados (null si no se subió archivo)
     */
    private List<Pedido> pedidos;
    
    /**
     * Verifica si los datos tienen más de X minutos
     */
    public boolean isExpired(int minutesToExpire) {
        if (createdAt == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(createdAt.plusMinutes(minutesToExpire));
    }
}

