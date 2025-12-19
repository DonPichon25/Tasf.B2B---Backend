package com.grupo5e.morapack.service;

import com.grupo5e.morapack.api.dto.FileValidationResult;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Vuelo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para almacenar temporalmente datos parseados de archivos
 * Los datos se almacenan en memoria y se limpian automáticamente después de 1 hora
 */
@Service
@Slf4j
public class TemporaryDataStorageService {
    
    private final Map<String, TemporarySimulationData> temporaryStorage = new ConcurrentHashMap<>();
    
    // Tiempo de expiración en minutos
    private static final int EXPIRATION_MINUTES = 60;
    
    /**
     * Almacena datos temporales parseados
     */
    public void storeTemporaryData(String sessionId,
                                   FileValidationResult aeropuertosResult,
                                   FileValidationResult vuelosResult,
                                   FileValidationResult pedidosResult) {
        
        List<Aeropuerto> aeropuertos = null;
        List<Vuelo> vuelos = null;
        List<Pedido> pedidos = null;
        
        // Extraer aeropuertos parseados
        if (aeropuertosResult != null && aeropuertosResult.isSuccess() && 
            aeropuertosResult.getParsedAeropuertos() != null) {
            aeropuertos = aeropuertosResult.getParsedAeropuertos();
            log.info("📦 Almacenando {} aeropuertos para sesión {}", aeropuertos.size(), sessionId);
        }
        
        // Extraer vuelos parseados
        if (vuelosResult != null && vuelosResult.isSuccess() && 
            vuelosResult.getParsedVuelos() != null) {
            vuelos = vuelosResult.getParsedVuelos();
            log.info("📦 Almacenando {} vuelos para sesión {}", vuelos.size(), sessionId);
        }
        
        // Extraer pedidos parseados
        if (pedidosResult != null && pedidosResult.isSuccess() && 
            pedidosResult.getParsedPedidos() != null) {
            pedidos = pedidosResult.getParsedPedidos();
            log.info("📦 Almacenando {} pedidos para sesión {}", pedidos.size(), sessionId);
        }
        
        // Crear y almacenar el objeto con los datos
        TemporarySimulationData data = TemporarySimulationData.builder()
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .aeropuertos(aeropuertos)
                .vuelos(vuelos)
                .pedidos(pedidos)
                .build();
        
        temporaryStorage.put(sessionId, data);
        log.info("✅ Datos temporales almacenados para sesión: {}", sessionId);
        log.info("   - Aeropuertos: {}", aeropuertos != null ? aeropuertos.size() : "usando BD");
        log.info("   - Vuelos: {}", vuelos != null ? vuelos.size() : "usando BD");
        log.info("   - Pedidos: {}", pedidos != null ? pedidos.size() : "usando BD");
    }
    
    /**
     * Almacena datos temporales directamente
     */
    public void storeTemporaryDataDirect(String sessionId,
                                        List<Aeropuerto> aeropuertos,
                                        List<Vuelo> vuelos,
                                        List<Pedido> pedidos) {
        
        TemporarySimulationData data = TemporarySimulationData.builder()
                .sessionId(sessionId)
                .createdAt(LocalDateTime.now())
                .aeropuertos(aeropuertos)
                .vuelos(vuelos)
                .pedidos(pedidos)
                .build();
        
        temporaryStorage.put(sessionId, data);
        
        log.info("✅ Datos temporales almacenados para sesión: {}", sessionId);
        log.info("   - Aeropuertos: {}", aeropuertos != null ? aeropuertos.size() : 0);
        log.info("   - Vuelos: {}", vuelos != null ? vuelos.size() : 0);
        log.info("   - Pedidos: {}", pedidos != null ? pedidos.size() : 0);
    }
    
    /**
     * Obtiene datos temporales por session ID
     */
    public TemporarySimulationData getTemporaryData(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }
        
        TemporarySimulationData data = temporaryStorage.get(sessionId);
        
        if (data == null) {
            log.warn("⚠️ No se encontraron datos temporales para sesión: {}", sessionId);
            return null;
        }
        
        // Verificar si los datos expiraron
        if (data.isExpired(EXPIRATION_MINUTES)) {
            log.warn("⚠️ Datos temporales expirados para sesión: {}", sessionId);
            temporaryStorage.remove(sessionId);
            return null;
        }
        
        log.info("✅ Datos temporales recuperados para sesión: {}", sessionId);
        return data;
    }
    
    /**
     * Limpia datos temporales de una sesión específica
     */
    public void clearTemporaryData(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }
        
        TemporarySimulationData removed = temporaryStorage.remove(sessionId);
        if (removed != null) {
            log.info("🗑️ Datos temporales eliminados para sesión: {}", sessionId);
        }
    }
    
    /**
     * Limpieza automática cada 30 minutos
     * Elimina datos que tengan más de 60 minutos
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 minutos
    public void cleanupExpiredData() {
        log.info("🧹 Iniciando limpieza automática de datos temporales...");
        
        List<String> expiredSessions = new ArrayList<>();
        
        for (Map.Entry<String, TemporarySimulationData> entry : temporaryStorage.entrySet()) {
            if (entry.getValue().isExpired(EXPIRATION_MINUTES)) {
                expiredSessions.add(entry.getKey());
            }
        }
        
        for (String sessionId : expiredSessions) {
            temporaryStorage.remove(sessionId);
            log.info("🗑️ Sesión expirada eliminada: {}", sessionId);
        }
        
        log.info("✅ Limpieza completada. {} sesiones eliminadas. {} sesiones activas.",
                expiredSessions.size(), temporaryStorage.size());
    }
    
    /**
     * Obtiene el número de sesiones activas
     */
    public int getActiveSessionsCount() {
        return temporaryStorage.size();
    }

    // Añade este método a tu TemporaryDataStorageService.java
    public void storeOrders(String sessionId, List<Pedido> orders) {
        // Obtenemos o creamos la sesión temporal
        TemporarySimulationData data = temporaryStorage.computeIfAbsent(sessionId,
                k -> new TemporarySimulationData());

        // Guardamos la lista completa en la sesión
        data.setPedidos(orders);
        data.setCreatedAt(LocalDateTime.now());

        log.info("💾 {} pedidos almacenados en RAM para la sesión {}", orders.size(), sessionId);
    }
}

