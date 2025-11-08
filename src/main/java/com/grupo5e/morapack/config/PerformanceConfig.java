package com.grupo5e.morapack.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Configuración de Performance para MoraPack.
 * 
 * Incluye:
 * - Thread pool para operaciones asíncronas
 * - Cache manager para consultas frecuentes
 * - Configuración de timeouts
 */
@Configuration
@EnableAsync
@EnableCaching
public class PerformanceConfig {

    /**
     * Executor para operaciones asíncronas (útil para algoritmo semanal)
     */
    @Bean(name = "algorithmExecutor")
    public Executor algorithmExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool: número de threads siempre activos
        executor.setCorePoolSize(4);
        
        // Max pool: máximo de threads simultáneos
        executor.setMaxPoolSize(16);
        
        // Queue capacity: tareas en espera
        executor.setQueueCapacity(100);
        
        // Thread name prefix para debugging
        executor.setThreadNamePrefix("algorithm-exec-");
        
        // Graceful shutdown: esperar a que terminen las tareas
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    /**
     * Cache Manager para consultas frecuentes.
     * 
     * Caches:
     * - aeropuertos: Lista de aeropuertos (cambia poco)
     * - vuelos: Lista de vuelos (cambia poco)
     * - consultas: Resultados de consultas del algoritmo
     */
    @Bean
    @ConditionalOnProperty(name = "morapack.cache.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        cacheManager.setCaches(Arrays.asList(
            // Cache para aeropuertos (TTL implícito por SimpleCacheManager)
            new ConcurrentMapCache("aeropuertos"),
            
            // Cache para vuelos
            new ConcurrentMapCache("vuelos"),
            
            // Cache para consultas de pedidos
            new ConcurrentMapCache("consultas-pedidos"),
            
            // Cache para estadísticas
            new ConcurrentMapCache("estadisticas")
        ));
        
        return cacheManager;
    }
}

