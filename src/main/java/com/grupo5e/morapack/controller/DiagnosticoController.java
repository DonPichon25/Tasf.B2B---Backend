package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.service.AeropuertoService;
import com.grupo5e.morapack.service.VueloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador para diagnosticar problemas con los datos
 */
@Slf4j
@RestController
@RequestMapping("/api/diagnostico")
@RequiredArgsConstructor
@Tag(name = "Diagnóstico", description = "Endpoints para diagnosticar problemas de datos")
public class DiagnosticoController {

    private final VueloService vueloService;
    private final AeropuertoService aeropuertoService;

    @Operation(summary = "Verificar estado completo de datos cargados")
    @GetMapping("/verificar-datos")
    public ResponseEntity<Map<String, Object>> verificarDatos() {
        log.info("🔍 Verificando estado completo de datos...");
        
        Map<String, Object> resultado = new HashMap<>();
        List<Vuelo> todosLosVuelos = vueloService.listar();
        List<Aeropuerto> todosLosAeropuertos = aeropuertoService.listar();
        
        // 1. Estadísticas de aeropuertos
        Map<String, Object> aeropuertosStats = new HashMap<>();
        aeropuertosStats.put("total", todosLosAeropuertos.size());
        
        List<Map<String, Object>> primeros5Aeropuertos = todosLosAeropuertos.stream()
            .limit(5)
            .map(a -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", a.getId());
                map.put("codigoIATA", a.getCodigoIATA());
                map.put("alias", a.getAlias() != null ? a.getAlias() : "N/A");
                return map;
            })
            .collect(Collectors.toList());
        
        aeropuertosStats.put("primeros5", primeros5Aeropuertos);
        
        // 2. Estadísticas de vuelos
        int vuelosSinOrigen = 0;
        int vuelosSinDestino = 0;
        int vuelosSinAmbos = 0;
        int vuelosCorrectos = 0;
        
        List<Map<String, Object>> primeros5Vuelos = new ArrayList<>();
        List<Map<String, Object>> vuelosProblematicos = new ArrayList<>();
        
        for (int i = 0; i < todosLosVuelos.size(); i++) {
            Vuelo vuelo = todosLosVuelos.get(i);
            boolean sinOrigen = vuelo.getAeropuertoOrigen() == null;
            boolean sinDestino = vuelo.getAeropuertoDestino() == null;
            
            if (sinOrigen && sinDestino) {
                vuelosSinAmbos++;
                if (vuelosProblematicos.size() < 5) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", vuelo.getId());
                    map.put("problema", "Sin origen ni destino");
                    map.put("horaSalida", vuelo.getHoraSalida() != null ? vuelo.getHoraSalida().toString() : "N/A");
                    vuelosProblematicos.add(map);
                }
            } else if (sinOrigen) {
                vuelosSinOrigen++;
                if (vuelosProblematicos.size() < 5) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", vuelo.getId());
                    map.put("problema", "Sin origen");
                    map.put("destino", vuelo.getAeropuertoDestino().getCodigoIATA());
                    vuelosProblematicos.add(map);
                }
            } else if (sinDestino) {
                vuelosSinDestino++;
                if (vuelosProblematicos.size() < 5) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", vuelo.getId());
                    map.put("problema", "Sin destino");
                    map.put("origen", vuelo.getAeropuertoOrigen().getCodigoIATA());
                    vuelosProblematicos.add(map);
                }
            } else {
                vuelosCorrectos++;
                if (primeros5Vuelos.size() < 5) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", vuelo.getId());
                    map.put("ruta", vuelo.getAeropuertoOrigen().getCodigoIATA() + " → " + 
                           vuelo.getAeropuertoDestino().getCodigoIATA());
                    map.put("horaSalida", vuelo.getHoraSalida() != null ? vuelo.getHoraSalida().toString() : "N/A");
                    map.put("capacidad", vuelo.getCapacidadMaxima());
                    primeros5Vuelos.add(map);
                }
            }
        }
        
        Map<String, Object> vuelosStats = new HashMap<>();
        vuelosStats.put("total", todosLosVuelos.size());
        vuelosStats.put("correctos", vuelosCorrectos);
        vuelosStats.put("sinOrigen", vuelosSinOrigen);
        vuelosStats.put("sinDestino", vuelosSinDestino);
        vuelosStats.put("sinAmbos", vuelosSinAmbos);
        vuelosStats.put("porcentajeCorrectos", 
            todosLosVuelos.isEmpty() ? 0 : (vuelosCorrectos * 100.0 / todosLosVuelos.size()));
        vuelosStats.put("primeros5Correctos", primeros5Vuelos);
        vuelosStats.put("primeros5Problematicos", vuelosProblematicos);
        
        // 3. Evaluación general
        boolean datosListos = todosLosAeropuertos.size() > 0 && 
                             vuelosCorrectos > 0 && 
                             vuelosCorrectos == todosLosVuelos.size();
        
        String mensaje;
        String estado;
        if (todosLosAeropuertos.isEmpty()) {
            estado = "ERROR";
            mensaje = "❌ No hay aeropuertos cargados. Sube airports.txt primero.";
        } else if (todosLosVuelos.isEmpty()) {
            estado = "WARNING";
            mensaje = "⚠️ Aeropuertos cargados pero no hay vuelos. Sube flights.txt.";
        } else if (vuelosCorrectos == 0) {
            estado = "ERROR";
            mensaje = "❌ Todos los vuelos tienen problemas. Verifica el formato del archivo.";
        } else if (vuelosCorrectos < todosLosVuelos.size()) {
            estado = "WARNING";
            mensaje = String.format("⚠️ %d/%d vuelos están correctos. Ejecuta /reparar-vuelos", 
                vuelosCorrectos, todosLosVuelos.size());
        } else {
            estado = "SUCCESS";
            mensaje = "✅ ¡Perfecto! Todos los datos están correctos. Puedes ejecutar la simulación.";
        }
        
        resultado.put("estado", estado);
        resultado.put("mensaje", mensaje);
        resultado.put("datosListos", datosListos);
        resultado.put("aeropuertos", aeropuertosStats);
        resultado.put("vuelos", vuelosStats);
        
        log.info("📊 Verificación completada: {}", estado);
        
        return ResponseEntity.ok(resultado);
    }
    
    @Operation(summary = "Diagnosticar vuelos sin aeropuertos")
    @GetMapping("/vuelos-sin-aeropuertos")
    public ResponseEntity<Map<String, Object>> diagnosticarVuelosSinAeropuertos() {
        log.info("🔍 Iniciando diagnóstico de vuelos sin aeropuertos...");
        
        Map<String, Object> resultado = new HashMap<>();
        List<Vuelo> todosLosVuelos = vueloService.listar();
        List<Aeropuerto> todosLosAeropuertos = aeropuertoService.listar();
        
        // Contar vuelos sin aeropuertos
        List<Map<String, Object>> vuelosProblematicos = new ArrayList<>();
        int vuelosSinOrigen = 0;
        int vuelosSinDestino = 0;
        int vuelosSinAmbos = 0;
        
        for (Vuelo vuelo : todosLosVuelos) {
            boolean sinOrigen = vuelo.getAeropuertoOrigen() == null;
            boolean sinDestino = vuelo.getAeropuertoDestino() == null;
            
            if (sinOrigen || sinDestino) {
                Map<String, Object> vueloInfo = new HashMap<>();
                vueloInfo.put("id", vuelo.getId());
                vueloInfo.put("identificador", vuelo.getIdentificadorVuelo());
                vueloInfo.put("origenId", vuelo.getAeropuertoOrigen() != null ? vuelo.getAeropuertoOrigen().getId() : null);
                vueloInfo.put("destinoId", vuelo.getAeropuertoDestino() != null ? vuelo.getAeropuertoDestino().getId() : null);
                vueloInfo.put("sinOrigen", sinOrigen);
                vueloInfo.put("sinDestino", sinDestino);
                
                vuelosProblematicos.add(vueloInfo);
                
                if (sinOrigen && sinDestino) vuelosSinAmbos++;
                else if (sinOrigen) vuelosSinOrigen++;
                else if (sinDestino) vuelosSinDestino++;
            }
        }
        
        // Información de aeropuertos
        List<Map<String, Object>> aeropuertosInfo = new ArrayList<>();
        for (Aeropuerto aeropuerto : todosLosAeropuertos) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", aeropuerto.getId());
            info.put("codigoIATA", aeropuerto.getCodigoIATA());
            info.put("alias", aeropuerto.getAlias());
            aeropuertosInfo.add(info);
        }
        
        // Construir resultado
        resultado.put("totalVuelos", todosLosVuelos.size());
        resultado.put("totalAeropuertos", todosLosAeropuertos.size());
        resultado.put("vuelosSinOrigen", vuelosSinOrigen);
        resultado.put("vuelosSinDestino", vuelosSinDestino);
        resultado.put("vuelosSinAmbos", vuelosSinAmbos);
        resultado.put("totalVuelosProblematicos", vuelosProblematicos.size());
        resultado.put("porcentajeProblematico", 
            todosLosVuelos.isEmpty() ? 0 : (vuelosProblematicos.size() * 100.0 / todosLosVuelos.size()));
        
        // Solo mostrar primeros 10 vuelos problemáticos
        resultado.put("vuelosProblematicos", 
            vuelosProblematicos.size() > 10 ? vuelosProblematicos.subList(0, 10) : vuelosProblematicos);
        resultado.put("aeropuertosDisponibles", aeropuertosInfo);
        
        log.info("📊 Diagnóstico completado:");
        log.info("   Total vuelos: {}", todosLosVuelos.size());
        log.info("   Vuelos sin origen: {}", vuelosSinOrigen);
        log.info("   Vuelos sin destino: {}", vuelosSinDestino);
        log.info("   Vuelos sin ambos: {}", vuelosSinAmbos);
        log.info("   Total problemáticos: {}", vuelosProblematicos.size());
        
        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Reparar vuelos sin aeropuertos usando identificadores")
    @PostMapping("/reparar-vuelos")
    public ResponseEntity<Map<String, Object>> repararVuelos() {
        log.info("🔧 Iniciando reparación de vuelos...");
        
        Map<String, Object> resultado = new HashMap<>();
        List<Vuelo> todosLosVuelos = vueloService.listar();
        List<Aeropuerto> todosLosAeropuertos = aeropuertoService.listar();
        
        // Crear mapa de código IATA -> Aeropuerto
        Map<String, Aeropuerto> aeropuertosPorCodigo = new HashMap<>();
        for (Aeropuerto aeropuerto : todosLosAeropuertos) {
            aeropuertosPorCodigo.put(aeropuerto.getCodigoIATA(), aeropuerto);
        }
        
        int vuelosReparados = 0;
        int vuelosNoReparables = 0;
        List<String> errores = new ArrayList<>();
        
        for (Vuelo vuelo : todosLosVuelos) {
            boolean necesitaReparacion = vuelo.getAeropuertoOrigen() == null || 
                                          vuelo.getAeropuertoDestino() == null;
            
            String identificador = vuelo.getIdentificadorVuelo();
            
            if (necesitaReparacion) {
                if (identificador == null) {
                    vuelosNoReparables++;
                    String error = String.format("Vuelo %d: No puede generar identificador (falta horaSalida u origen/destino)", 
                        vuelo.getId());
                    errores.add(error);
                    log.warn("   ❌ {}", error);
                    continue;
                }
                
                // Identificador formato: ORIGEN-DESTINO-HH:MM
                String[] partes = identificador.split("-");
                
                if (partes.length >= 2) {
                    String codigoOrigen = partes[0];
                    String codigoDestino = partes[1];
                    
                    Aeropuerto origen = aeropuertosPorCodigo.get(codigoOrigen);
                    Aeropuerto destino = aeropuertosPorCodigo.get(codigoDestino);
                    
                    if (origen != null && destino != null) {
                        vuelo.setAeropuertoOrigen(origen);
                        vuelo.setAeropuertoDestino(destino);
                        vueloService.actualizar(vuelo.getId(), vuelo);
                        vuelosReparados++;
                        log.info("   ✅ Reparado vuelo {}: {} -> {}", 
                            vuelo.getId(), codigoOrigen, codigoDestino);
                    } else {
                        vuelosNoReparables++;
                        String error = String.format("Vuelo %d: No se encontraron aeropuertos %s o %s", 
                            vuelo.getId(), codigoOrigen, codigoDestino);
                        errores.add(error);
                        log.warn("   ❌ {}", error);
                    }
                } else {
                    vuelosNoReparables++;
                    String error = String.format("Vuelo %d: Identificador inválido '%s'", 
                        vuelo.getId(), identificador);
                    errores.add(error);
                }
            }
        }
        
        resultado.put("vuelosReparados", vuelosReparados);
        resultado.put("vuelosNoReparables", vuelosNoReparables);
        resultado.put("errores", errores.size() > 10 ? errores.subList(0, 10) : errores);
        resultado.put("success", vuelosReparados > 0);
        resultado.put("message", String.format("Se repararon %d vuelos. %d no se pudieron reparar.", 
            vuelosReparados, vuelosNoReparables));
        
        log.info("✅ Reparación completada: {} vuelos reparados, {} no reparables", 
            vuelosReparados, vuelosNoReparables);
        
        return ResponseEntity.ok(resultado);
    }
}

