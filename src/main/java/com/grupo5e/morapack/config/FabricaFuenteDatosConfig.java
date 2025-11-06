package com.grupo5e.morapack.config;

import com.grupo5e.morapack.algorithm.input.FabricaFuenteDatos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Configuración Spring para inyectar ApplicationContext en FabricaFuenteDatos.
 * Permite que el algoritmo use modo BASEDATOS cuando Spring esté disponible.
 * 
 * Esta configuración se ejecuta automáticamente al iniciar la aplicación Spring.
 */
@Component
public class FabricaFuenteDatosConfig {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    /**
     * Configura la fábrica con el contexto de Spring al iniciar la aplicación.
     * Esto permite que FabricaFuenteDatos cree instancias de FuenteDatosBaseDatos
     * con inyección de dependencias de Spring.
     */
    @PostConstruct
    public void configurarFabrica() {
        FabricaFuenteDatos.setSpringContext(applicationContext);
        System.out.println("✅ FabricaFuenteDatos configurada con ApplicationContext de Spring");
        System.out.println("   Modo BASEDATOS disponible: " + FabricaFuenteDatos.esModoBaseDatosDisponible());
    }
}

