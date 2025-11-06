package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.core.validation.EntityValidator;
import com.grupo5e.morapack.core.validation.PACKTimeValidator;
import com.grupo5e.morapack.repository.CiudadRepository;
import com.grupo5e.morapack.repository.ClienteRepository;
import com.grupo5e.morapack.repository.ProductoRepository;
import com.grupo5e.morapack.utils.LectorAeropuerto;
import com.grupo5e.morapack.utils.LectorPedidos;
import com.grupo5e.morapack.utils.LectorVuelos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para importar datos desde archivos .txt
 * Reutiliza los Lectores existentes (LectorAeropuerto, LectorVuelos, LectorPedidos)
 * y guarda los datos directamente en BD (sin IDs temporales ni sessionId)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataImportService {

    private final AeropuertoService aeropuertoService;
    private final VueloService vueloService;
    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final CiudadRepository ciudadRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final com.grupo5e.morapack.repository.AlmacenRepository almacenRepository;
    
    /**
     * Guarda MultipartFile a archivo temporal
     */
    private Path guardarArchivoTemporal(MultipartFile file) throws IOException {
        Path tempFile = Files.createTempFile("morapack-import-", ".txt");
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }
    
    /**
     * Elimina archivo temporal
     */
    private void eliminarArchivoTemporal(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar archivo temporal: {}", path, e);
        }
    }

    /**
     * Importa aeropuertos desde archivo .txt y los guarda en BD inmediatamente
     * 
     * @param file Archivo aeropuertosinfo.txt
     * @return Map con success, message, count, cities
     */
    @Transactional
    public Map<String, Object> importAirports(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        Path tempFile = null;
        try {
            log.info("🏢 Iniciando importación de aeropuertos...");
            
            // 1. Guardar archivo temporal y usar LectorAeropuerto
            tempFile = guardarArchivoTemporal(file);
            LectorAeropuerto lector = new LectorAeropuerto(tempFile.toString());
            ArrayList<Aeropuerto> aeropuertos = lector.leerAeropuertos();
            
            if (aeropuertos.isEmpty()) {
                result.put("success", false);
                result.put("message", "No se encontraron aeropuertos en el archivo");
                result.put("count", 0);
                log.warn("❌ No se encontraron aeropuertos en el archivo");
                return result;
            }
            
            log.info("   Aeropuertos parseados: {}", aeropuertos.size());
            
            // 2. Validar aeropuertos antes de guardar
            try {
                EntityValidator.validateAeropuertos(aeropuertos);
                log.info("   ✅ Validación de aeropuertos exitosa");
            } catch (IllegalArgumentException e) {
                log.error("   ❌ Error de validación en aeropuertos: {}", e.getMessage());
                result.put("success", false);
                result.put("message", "Error de validación: " + e.getMessage());
                result.put("count", 0);
                return result;
            }
            
            // 3. Extraer ciudades únicas, validar y guardar en BD
            Set<Ciudad> ciudadesUnicas = aeropuertos.stream()
                .map(Aeropuerto::getCiudad)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            // Validar ciudades
            for (Ciudad ciudad : ciudadesUnicas) {
                EntityValidator.validateCiudad(ciudad);
            }
            
            List<Ciudad> ciudadesGuardadas = ciudadRepository.saveAll(new ArrayList<>(ciudadesUnicas));
            log.info("   ✅ {} ciudades guardadas en BD", ciudadesGuardadas.size());
            
            // 4. Actualizar referencias de ciudad en aeropuertos con IDs reales de BD
            Map<String, Ciudad> mapaCiudades = ciudadesGuardadas.stream()
                .collect(Collectors.toMap(
                    c -> c.getNombre() + "-" + c.getContinente(),
                    c -> c
                ));
            
            for (Aeropuerto aeropuerto : aeropuertos) {
                Ciudad ciudad = aeropuerto.getCiudad();
                if (ciudad != null) {
                    String key = ciudad.getNombre() + "-" + ciudad.getContinente();
                    Ciudad ciudadGuardada = mapaCiudades.get(key);
                    if (ciudadGuardada != null) {
                        aeropuerto.setCiudad(ciudadGuardada);
                    }
                }
            }
            
            // 5. Guardar aeropuertos en BD (IDs auto-generados)
            List<Aeropuerto> aeropuertosGuardados = aeropuertoService.insertarBulk(aeropuertos);
            
            // 6. Crear almacenes para cada aeropuerto (siguiendo patrón Backend)
            List<com.grupo5e.morapack.core.model.Almacen> almacenes = new ArrayList<>();
            for (Aeropuerto aeropuerto : aeropuertosGuardados) {
                com.grupo5e.morapack.core.model.Almacen almacen = com.grupo5e.morapack.core.model.Almacen.builder()
                    .nombre("Almacen " + aeropuerto.getCodigoIATA())
                    .capacidadMaxima(1000) // Capacidad por defecto
                    .capacidadUsada(0)
                    .esAlmacenPrincipal(false)
                    .build();
                almacenes.add(almacen);
            }
            
            List<com.grupo5e.morapack.core.model.Almacen> almacenesGuardados = almacenRepository.saveAll(almacenes);
            log.info("   ✅ {} almacenes creados para aeropuertos", almacenesGuardados.size());
            
            // 7. Asociar almacenes a aeropuertos (bidireccional)
            for (int i = 0; i < aeropuertosGuardados.size(); i++) {
                Aeropuerto aero = aeropuertosGuardados.get(i);
                com.grupo5e.morapack.core.model.Almacen alm = almacenesGuardados.get(i);
                // Establecer relación bidireccional
                aero.setAlmacen(alm);
                alm.setAeropuerto(aero);
            }
            // Re-guardar para persistir la relación
            almacenRepository.saveAll(almacenesGuardados);
            aeropuertoService.insertarBulk(aeropuertosGuardados);
            
            result.put("success", true);
            result.put("message", "Aeropuertos y almacenes importados exitosamente");
            result.put("count", aeropuertosGuardados.size());
            result.put("cities", ciudadesGuardadas.size());
            result.put("almacenes", almacenesGuardados.size());
            
            log.info("✅ {} aeropuertos y {} almacenes importados con IDs: {}", 
                     aeropuertosGuardados.size(),
                     almacenesGuardados.size(),
                     aeropuertosGuardados.stream()
                         .limit(3)
                         .map(a -> a.getId() + ":" + a.getCodigoIATA())
                         .collect(Collectors.joining(", ")));
            
        } catch (Exception e) {
            log.error("❌ Error importando aeropuertos", e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        } finally {
            eliminarArchivoTemporal(tempFile);
        }
        
        return result;
    }

    /**
     * Importa vuelos desde archivo .txt y los guarda en BD inmediatamente
     * Requiere que existan aeropuertos en BD
     * 
     * @param file Archivo vuelos.txt
     * @return Map con success, message, count
     */
    @Transactional
    public Map<String, Object> importFlights(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        Path tempFile = null;
        
        try {
            log.info("✈️ Iniciando importación de vuelos...");
            
            // 1. Verificar que existan aeropuertos en BD
            List<Aeropuerto> aeropuertos = aeropuertoService.listar();
            if (aeropuertos.isEmpty()) {
                result.put("success", false);
                result.put("message", "Debe importar aeropuertos primero antes de importar vuelos");
                result.put("count", 0);
                log.warn("❌ No hay aeropuertos en BD. Importe aeropuertos primero.");
                return result;
            }
            
            log.info("   Aeropuertos disponibles en BD: {}", aeropuertos.size());
            
            // 2. Guardar archivo temporal y usar LectorVuelos
            tempFile = guardarArchivoTemporal(file);
            LectorVuelos lector = new LectorVuelos(
                tempFile.toString(),
                new ArrayList<>(aeropuertos)
            );
            ArrayList<Vuelo> vuelos = lector.leerVuelos();
            
            if (vuelos.isEmpty()) {
                result.put("success", false);
                result.put("message", "No se encontraron vuelos en el archivo");
                result.put("count", 0);
                log.warn("❌ No se encontraron vuelos en el archivo");
                return result;
            }
            
            log.info("   Vuelos parseados: {}", vuelos.size());
            
            // 3. Validar vuelos antes de guardar
            try {
                EntityValidator.validateVuelos(vuelos);
                log.info("   ✅ Validación de vuelos exitosa");
            } catch (IllegalArgumentException e) {
                log.error("   ❌ Error de validación en vuelos: {}", e.getMessage());
                result.put("success", false);
                result.put("message", "Error de validación: " + e.getMessage());
                result.put("count", 0);
                return result;
            }
            
            // 5. Validar tiempos PACK (con advertencias, no bloquear)
            int vuelosPACKCompliant = 0;
            for (Vuelo vuelo : vuelos) {
                if (PACKTimeValidator.validateFullPACKCompliance(vuelo)) {
                    vuelosPACKCompliant++;
                }
            }
            log.info("   📊 {} de {} vuelos cumplen estándares PACK ({}%)", 
                vuelosPACKCompliant, vuelos.size(), 
                vuelos.size() > 0 ? (vuelosPACKCompliant * 100 / vuelos.size()) : 0);
            
            // 4. Guardar vuelos en BD (IDs auto-generados)
            List<Vuelo> vuelosGuardados = vueloService.insertarBulk(vuelos);
            
            result.put("success", true);
            result.put("message", "Vuelos importados exitosamente");
            result.put("count", vuelosGuardados.size());
            
            log.info("✅ {} vuelos importados con IDs: {}", 
                     vuelosGuardados.size(),
                     vuelosGuardados.stream()
                         .limit(3)
                         .map(v -> v.getId() + ":" + v.getAeropuertoOrigen().getCodigoIATA() + "-" + v.getAeropuertoDestino().getCodigoIATA())
                         .collect(Collectors.joining(", ")));
            
        } catch (Exception e) {
            log.error("❌ Error importando vuelos", e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        } finally {
            eliminarArchivoTemporal(tempFile);
        }
        
        return result;
    }

    /**
     * Importa pedidos desde archivo .txt y los guarda en BD inmediatamente
     * Requiere que existan aeropuertos en BD
     * NOTA: LectorPedidos ya guarda en BD internamente usando pedidoService
     * 
     * @param file Archivo pedidos.txt
     * @return Map con success, message, count
     */
    @Transactional
    public Map<String, Object> importOrders(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        Path tempFile = null;
        
        try {
            log.info("📦 Iniciando importación de pedidos...");
            
            // 1. Verificar que existan aeropuertos en BD
            List<Aeropuerto> aeropuertos = aeropuertoService.listar();
            if (aeropuertos.isEmpty()) {
                result.put("success", false);
                result.put("message", "Debe importar aeropuertos primero antes de importar pedidos");
                result.put("count", 0);
                log.warn("❌ No hay aeropuertos en BD. Importe aeropuertos primero.");
                return result;
            }
            
            log.info("   Aeropuertos disponibles en BD: {}", aeropuertos.size());
            
            // 2. Contar pedidos antes de importar
            int countAntes = pedidoService.listar().size();
            log.info("   Pedidos en BD antes de importar: {}", countAntes);
            
            // 3. Guardar archivo temporal y usar LectorPedidos
            // NOTA: LectorPedidos ya guarda en BD directamente usando pedidoService.insertar()
            tempFile = guardarArchivoTemporal(file);
            LectorPedidos lector = new LectorPedidos(
                tempFile.toString(),
                new ArrayList<>(aeropuertos),
                pedidoService,
                clienteService
            );
            lector.leerYGuardarProductos();
            
            // 4. Contar pedidos después de importar
            int countDespues = pedidoService.listar().size();
            int pedidosImportados = countDespues - countAntes;
            
            log.info("   Pedidos en BD después de importar: {}", countDespues);
            
            result.put("success", true);
            result.put("message", "Pedidos importados exitosamente");
            result.put("count", pedidosImportados);
            
            log.info("✅ {} pedidos importados", pedidosImportados);
            
        } catch (Exception e) {
            log.error("❌ Error importando pedidos", e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        } finally {
            eliminarArchivoTemporal(tempFile);
        }
        
        return result;
    }

    // ========== DATABASE STATISTICS ==========
    
    /**
     * Obtiene estadísticas actuales de la base de datos
     */
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long aeropuertos = aeropuertoService.listar().size();
            long vuelos = vueloService.listar().size();
            long pedidos = pedidoService.listar().size();
            long ciudades = ciudadRepository.count();
            long clientes = clienteRepository.count();
            long productos = productoRepository.count();
            
            stats.put("aeropuertos", aeropuertos);
            stats.put("vuelos", vuelos);
            stats.put("pedidos", pedidos);
            stats.put("ciudades", ciudades);
            stats.put("clientes", clientes);
            stats.put("productos", productos);
            stats.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    // ========== DATABASE CLEANUP METHODS ==========
    
    /**
     * Limpia TODA la base de datos (respetando foreign keys)
     * Orden: Simulaciones → Productos → Pedidos → Vuelos → Aeropuertos → Ciudades
     */
    @Transactional
    public Map<String, Object> clearAllData() {
        Map<String, Object> result = new HashMap<>();
        int totalDeleted = 0;
        
        try {
            log.warn("⚠️ Iniciando limpieza COMPLETA de base de datos");
            
            // 1. Limpiar simulaciones primero (tienen FK a pedidos y vuelos)
            long simDeleted = clearSimulationsOnly();
            totalDeleted += simDeleted;
            log.info("  - Simulaciones eliminadas: {}", simDeleted);
            
            // 2. Limpiar productos (tienen FK a pedidos)
            long prodDeleted = productoRepository.count();
            productoRepository.deleteAll();
            totalDeleted += prodDeleted;
            log.info("  - Productos eliminados: {}", prodDeleted);
            
            // 3. Limpiar pedidos (tienen FK a aeropuertos/ciudades)
            long pedDeleted = pedidoService.listar().size();
            pedidoService.listar().forEach(pedido -> pedidoService.eliminar(pedido.getId()));
            totalDeleted += pedDeleted;
            log.info("  - Pedidos eliminados: {}", pedDeleted);
            
            // 4. Limpiar vuelos (tienen FK a aeropuertos)
            long vuelosDeleted = vueloService.listar().size();
            vueloService.listar().forEach(vuelo -> vueloService.eliminar(vuelo.getId()));
            totalDeleted += vuelosDeleted;
            log.info("  - Vuelos eliminados: {}", vuelosDeleted);
            
            // 5. Limpiar aeropuertos (tienen FK a ciudades)
            long aeroDeleted = aeropuertoService.listar().size();
            aeropuertoService.listar().forEach(aero -> aeropuertoService.eliminar(aero.getId()));
            totalDeleted += aeroDeleted;
            log.info("  - Aeropuertos eliminados: {}", aeroDeleted);
            
            // 6. Limpiar ciudades (no tienen FK)
            long ciudadesDeleted = ciudadRepository.count();
            ciudadRepository.deleteAll();
            totalDeleted += ciudadesDeleted;
            log.info("  - Ciudades eliminadas: {}", ciudadesDeleted);
            
            // 7. Limpiar clientes
            long clientesDeleted = clienteRepository.count();
            clienteRepository.deleteAll();
            totalDeleted += clientesDeleted;
            log.info("  - Clientes eliminados: {}", clientesDeleted);
            
            result.put("success", true);
            result.put("message", "Base de datos limpiada completamente");
            result.put("totalDeleted", totalDeleted);
            result.put("breakdown", Map.of(
                "simulaciones", simDeleted,
                "productos", prodDeleted,
                "pedidos", pedDeleted,
                "vuelos", vuelosDeleted,
                "aeropuertos", aeroDeleted,
                "ciudades", ciudadesDeleted,
                "clientes", clientesDeleted
            ));
            
            log.warn("✅ Limpieza COMPLETA finalizada. Total registros eliminados: {}", totalDeleted);
            
        } catch (Exception e) {
            log.error("❌ Error durante limpieza completa: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error al limpiar base de datos: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }
    
    /**
     * Limpia solo pedidos y productos
     */
    @Transactional
    public Map<String, Object> clearOrders() {
        Map<String, Object> result = new HashMap<>();
        int totalDeleted = 0;
        
        try {
            log.warn("⚠️ Iniciando limpieza de PEDIDOS");
            
            // 1. Primero limpiar simulaciones que dependen de pedidos
            long simDeleted = clearSimulationsOnly();
            totalDeleted += simDeleted;
            
            // 2. Limpiar productos
            long prodDeleted = productoRepository.count();
            productoRepository.deleteAll();
            totalDeleted += prodDeleted;
            
            // 3. Limpiar pedidos
            long pedDeleted = pedidoService.listar().size();
            pedidoService.listar().forEach(pedido -> pedidoService.eliminar(pedido.getId()));
            totalDeleted += pedDeleted;
            
            result.put("success", true);
            result.put("message", "Pedidos eliminados exitosamente");
            result.put("deleted", totalDeleted);
            result.put("breakdown", Map.of(
                "simulaciones", simDeleted,
                "productos", prodDeleted,
                "pedidos", pedDeleted
            ));
            
            log.info("✅ Pedidos eliminados. Total: {}", totalDeleted);
            
        } catch (Exception e) {
            log.error("❌ Error eliminando pedidos: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error al eliminar pedidos: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }
    
    /**
     * Limpia solo vuelos
     */
    @Transactional
    public Map<String, Object> clearFlights() {
        Map<String, Object> result = new HashMap<>();
        int totalDeleted = 0;
        
        try {
            log.warn("⚠️ Iniciando limpieza de VUELOS");
            
            // 1. Primero limpiar simulaciones que dependen de vuelos
            long simDeleted = clearSimulationsOnly();
            totalDeleted += simDeleted;
            
            // 2. Limpiar vuelos
            long vuelosDeleted = vueloService.listar().size();
            vueloService.listar().forEach(vuelo -> vueloService.eliminar(vuelo.getId()));
            totalDeleted += vuelosDeleted;
            
            result.put("success", true);
            result.put("message", "Vuelos eliminados exitosamente");
            result.put("deleted", totalDeleted);
            result.put("breakdown", Map.of(
                "simulaciones", simDeleted,
                "vuelos", vuelosDeleted
            ));
            
            log.info("✅ Vuelos eliminados. Total: {}", totalDeleted);
            
        } catch (Exception e) {
            log.error("❌ Error eliminando vuelos: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error al eliminar vuelos: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }
    
    /**
     * Limpia aeropuertos (y todo lo que depende de ellos)
     */
    @Transactional
    public Map<String, Object> clearAirports() {
        Map<String, Object> result = new HashMap<>();
        int totalDeleted = 0;
        
        try {
            log.warn("⚠️ Iniciando limpieza de AEROPUERTOS (incluye datos dependientes)");
            
            // 1. Limpiar simulaciones
            long simDeleted = clearSimulationsOnly();
            totalDeleted += simDeleted;
            
            // 2. Limpiar productos
            long prodDeleted = productoRepository.count();
            productoRepository.deleteAll();
            totalDeleted += prodDeleted;
            
            // 3. Limpiar pedidos
            long pedDeleted = pedidoService.listar().size();
            pedidoService.listar().forEach(pedido -> pedidoService.eliminar(pedido.getId()));
            totalDeleted += pedDeleted;
            
            // 4. Limpiar vuelos
            long vuelosDeleted = vueloService.listar().size();
            vueloService.listar().forEach(vuelo -> vueloService.eliminar(vuelo.getId()));
            totalDeleted += vuelosDeleted;
            
            // 5. Limpiar aeropuertos
            long aeroDeleted = aeropuertoService.listar().size();
            aeropuertoService.listar().forEach(aero -> aeropuertoService.eliminar(aero.getId()));
            totalDeleted += aeroDeleted;
            
            // 6. Limpiar ciudades
            long ciudadesDeleted = ciudadRepository.count();
            ciudadRepository.deleteAll();
            totalDeleted += ciudadesDeleted;
            
            result.put("success", true);
            result.put("message", "Aeropuertos y datos dependientes eliminados exitosamente");
            result.put("deleted", totalDeleted);
            result.put("breakdown", Map.of(
                "simulaciones", simDeleted,
                "productos", prodDeleted,
                "pedidos", pedDeleted,
                "vuelos", vuelosDeleted,
                "aeropuertos", aeroDeleted,
                "ciudades", ciudadesDeleted
            ));
            
            log.info("✅ Aeropuertos y dependencias eliminados. Total: {}", totalDeleted);
            
        } catch (Exception e) {
            log.error("❌ Error eliminando aeropuertos: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error al eliminar aeropuertos: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }
    
    /**
     * Limpia solo simulaciones (mantiene datos base)
     */
    @Transactional
    public Map<String, Object> clearSimulations() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.warn("⚠️ Iniciando limpieza de SIMULACIONES");
            
            long deleted = clearSimulationsOnly();
            
            result.put("success", true);
            result.put("message", "Simulaciones eliminadas exitosamente");
            result.put("deleted", deleted);
            
            log.info("✅ Simulaciones eliminadas. Total: {}", deleted);
            
        } catch (Exception e) {
            log.error("❌ Error eliminando simulaciones: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error al eliminar simulaciones: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }
        
        return result;
    }
    
    /**
     * Método auxiliar para limpiar simulaciones
     * Usado internamente por otros métodos de limpieza
     */
    private long clearSimulationsOnly() {
        // Aquí irían las llamadas a los repositorios de SimulacionSemanal y SimulacionAsignacion
        // Por ahora retorna 0 hasta que identifiquemos los repositorios correctos
        // TODO: Agregar cuando tengamos acceso a SimulacionSemanalRepository y SimulacionAsignacionRepository
        log.warn("TODO: Implementar limpieza de simulaciones cuando tengamos los repositorios");
        return 0;
    }
}

