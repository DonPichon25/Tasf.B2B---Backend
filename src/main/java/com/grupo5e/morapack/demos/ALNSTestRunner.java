package com.grupo5e.morapack.demos;

import com.grupo5e.morapack.algorithm.alns.ALNSSolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Test runner para ALNSSolver.
 * Actualizado para usar el nuevo constructor sin dependencias de Spring Services.
 * ALNSSolver ahora carga datos usando FuenteDatosInput modular (archivo o BD).
 */
@Component
public class ALNSTestRunner implements CommandLineRunner {
    
    @Value("${alns.test.enabled:false}")
    private boolean alnsTestEnabled;

    @Override
    public void run(String... args) throws Exception {
        // Solo ejecutar si está habilitado
        if (!alnsTestEnabled) {
            System.out.println("⏭️  ALNS Test deshabilitado (usa alns.test.enabled=true para ejecutarlo)");
            return;
        }
        
        System.out.println("🚀 INICIANDO PRUEBA DEL ALNSSOLVER 🚀");
        System.out.println("📂 ALNSSolver cargará datos desde FuenteDatosInput modular");
        System.out.println("   (Modo configurado en MODO_FUENTE_DATOS env var: ARCHIVO o BASEDATOS)");

        try {

//            // Verificar datos
//            System.out.println("\n=== VERIFICANDO DATOS ===");
//            var aeropuertos = aeropuertoService.listar();
//            var pedidos = pedidoService.listar();
//
//            System.out.println("Aeropuertos en BD: " + aeropuertos.size());
//            System.out.println("Pedidos en BD: " + pedidos.size());

//            if (aeropuertos.isEmpty() || pedidos.isEmpty()) {
//                System.out.println("⚠️  ADVERTENCIA: Pocos datos en la BD");
//                System.out.println("   - Considera agregar datos de prueba");
//            }

//            // Mostrar muestra de datos
//            System.out.println("\n=== MUESTRA DE DATOS ===");
//            System.out.println("Primeros 3 aeropuertos:");
//            aeropuertos.stream().limit(3).forEach(a ->
//                    System.out.println("  - " + a.getCodigoIATA() + " - " +
//                            (a.getCiudad() != null ? a.getCiudad().getNombre() : "Sin ciudad")));
//
//            System.out.println("\nPrimeros 3 pedidos:");
//            pedidos.stream().limit(3).forEach(p ->
//                    System.out.println("  - Pedido " + p.getId() + ": " +
//                            p.getAeropuertoOrigenCodigo() + " → " + p.getAeropuertoDestinoCodigo()));

            // Crear solver con nuevo constructor simplificado
            // ALNSSolver ahora carga datos automáticamente desde FuenteDatosInput
            System.out.println("\n=== INICIALIZANDO ALNSSOLVER ===");
            System.out.println("Usando constructor simplificado con 100 iteraciones");
            ALNSSolver solver = new ALNSSolver(100); // Menos iteraciones para prueba rápida

            // Ejecutar algoritmo
            System.out.println("\n=== EJECUTANDO ALGORITMO ===");
            long startTime = System.currentTimeMillis();

            solver.resolver();

            long endTime = System.currentTimeMillis();
            System.out.println("⏱️  Tiempo de ejecución: " + (endTime - startTime) + "ms");

            // Validar resultados
            System.out.println("\n=== VALIDACIÓN ===");
            boolean valida = solver.esSolucionValida();
            boolean capacidadValida = solver.esSolucionCapacidadValida();

            System.out.println("Solución válida: " + (valida ? "✅ SÍ" : "❌ NO"));
            System.out.println("Capacidades respetadas: " + (capacidadValida ? "✅ SÍ" : "❌ NO"));

            if (valida && capacidadValida) {
                System.out.println("🎉 ¡PRUEBA EXITOSA!");
            } else {
                System.out.println("⚠️  ADVERTENCIA: La solución tiene problemas de validación");
            }

        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}