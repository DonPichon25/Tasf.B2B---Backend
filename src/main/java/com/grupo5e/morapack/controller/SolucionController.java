package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.EstadisticasSolucionesDTO;
import com.grupo5e.morapack.api.dto.SolucionDTO;
import com.grupo5e.morapack.service.SolucionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para gestionar soluciones del algoritmo ALNS.
 */
@Slf4j
@RestController
@RequestMapping("/api/soluciones")
@RequiredArgsConstructor
@Tag(name = "Soluciones", description = "API para gestionar soluciones del algoritmo ALNS")
public class SolucionController {
    
    private final SolucionService solucionService;
    
    /**
     * Crear una nueva solución
     */
    @PostMapping
    @Operation(summary = "Crear solución", description = "Crea una nueva solución del algoritmo ALNS")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Solución creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<Integer> crearSolucion(@RequestBody SolucionDTO solucionDTO) {
        log.info("POST /api/soluciones - Crear solución del algoritmo: {}", solucionDTO.getAlgoritmoUsado());
        try {
            Integer id = solucionService.crearSolucion(solucionDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(id);
        } catch (RuntimeException e) {
            log.error("Error al crear solución: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
    
    /**
     * Obtener solución por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener solución", description = "Obtiene una solución específica por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solución encontrada"),
        @ApiResponse(responseCode = "404", description = "Solución no encontrada")
    })
    public ResponseEntity<SolucionDTO> obtenerPorId(
        @Parameter(description = "ID de la solución") @PathVariable Integer id) {
        log.info("GET /api/soluciones/{} - Obtener solución", id);
        try {
            SolucionDTO solucion = solucionService.obtenerPorId(id);
            return ResponseEntity.ok(solucion);
        } catch (RuntimeException e) {
            log.error("Solución no encontrada: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Listar todas las soluciones
     */
    @GetMapping
    @Operation(summary = "Listar soluciones", description = "Obtiene todas las soluciones registradas")
    @ApiResponse(responseCode = "200", description = "Lista de soluciones")
    public ResponseEntity<List<SolucionDTO>> obtenerTodas() {
        log.info("GET /api/soluciones - Listar todas");
        List<SolucionDTO> soluciones = solucionService.obtenerTodas();
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Obtener la mejor solución
     */
    @GetMapping("/mejor")
    @Operation(summary = "Mejor solución", description = "Obtiene la solución con mayor fitness")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mejor solución"),
        @ApiResponse(responseCode = "404", description = "No hay soluciones disponibles")
    })
    public ResponseEntity<SolucionDTO> obtenerMejor() {
        log.info("GET /api/soluciones/mejor - Obtener mejor solución");
        try {
            SolucionDTO solucion = solucionService.obtenerMejorSolucion();
            return ResponseEntity.ok(solucion);
        } catch (RuntimeException e) {
            log.error("No hay soluciones: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Obtener top N mejores soluciones
     */
    @GetMapping("/top/{limite}")
    @Operation(summary = "Top soluciones", description = "Obtiene las N mejores soluciones ordenadas por fitness")
    @ApiResponse(responseCode = "200", description = "Top soluciones")
    public ResponseEntity<List<SolucionDTO>> obtenerTopMejores(
        @Parameter(description = "Número de soluciones a retornar") @PathVariable int limite) {
        log.info("GET /api/soluciones/top/{} - Top mejores", limite);
        List<SolucionDTO> soluciones = solucionService.obtenerTopMejores(limite);
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Obtener soluciones por algoritmo
     */
    @GetMapping("/algoritmo/{algoritmo}")
    @Operation(summary = "Soluciones por algoritmo", description = "Filtra soluciones por algoritmo usado")
    @ApiResponse(responseCode = "200", description = "Soluciones del algoritmo especificado")
    public ResponseEntity<List<SolucionDTO>> obtenerPorAlgoritmo(
        @Parameter(description = "Nombre del algoritmo (ALNS)") @PathVariable String algoritmo) {
        log.info("GET /api/soluciones/algoritmo/{}", algoritmo);
        List<SolucionDTO> soluciones = solucionService.obtenerPorAlgoritmo(algoritmo);
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Filtrar soluciones por rango de costo
     */
    @GetMapping("/costo")
    @Operation(summary = "Soluciones por costo", description = "Filtra soluciones en un rango de costo")
    @ApiResponse(responseCode = "200", description = "Soluciones en el rango de costo")
    public ResponseEntity<List<SolucionDTO>> obtenerPorRangoCosto(
        @Parameter(description = "Costo mínimo") @RequestParam Double min,
        @Parameter(description = "Costo máximo") @RequestParam Double max) {
        log.info("GET /api/soluciones/costo?min={}&max={}", min, max);
        List<SolucionDTO> soluciones = solucionService.obtenerPorRangoCosto(min, max);
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Filtrar soluciones por rango de tiempo
     */
    @GetMapping("/tiempo")
    @Operation(summary = "Soluciones por tiempo", description = "Filtra soluciones en un rango de tiempo")
    @ApiResponse(responseCode = "200", description = "Soluciones en el rango de tiempo")
    public ResponseEntity<List<SolucionDTO>> obtenerPorRangoTiempo(
        @Parameter(description = "Tiempo mínimo en horas") @RequestParam Double min,
        @Parameter(description = "Tiempo máximo en horas") @RequestParam Double max) {
        log.info("GET /api/soluciones/tiempo?min={}&max={}", min, max);
        List<SolucionDTO> soluciones = solucionService.obtenerPorRangoTiempo(min, max);
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Filtrar soluciones por rango de fitness
     */
    @GetMapping("/fitness")
    @Operation(summary = "Soluciones por fitness", description = "Filtra soluciones en un rango de fitness")
    @ApiResponse(responseCode = "200", description = "Soluciones en el rango de fitness")
    public ResponseEntity<List<SolucionDTO>> obtenerPorRangoFitness(
        @Parameter(description = "Fitness mínimo") @RequestParam Double min,
        @Parameter(description = "Fitness máximo") @RequestParam Double max) {
        log.info("GET /api/soluciones/fitness?min={}&max={}", min, max);
        List<SolucionDTO> soluciones = solucionService.obtenerPorRangoFitness(min, max);
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Filtrar soluciones por máximo de no entregados
     */
    @GetMapping("/no-entregados")
    @Operation(summary = "Soluciones por no entregados", description = "Filtra soluciones con máximo de paquetes no entregados")
    @ApiResponse(responseCode = "200", description = "Soluciones con máximo de no entregados")
    public ResponseEntity<List<SolucionDTO>> obtenerPorMaxNoEntregados(
        @Parameter(description = "Máximo de paquetes no entregados permitidos") @RequestParam Integer max) {
        log.info("GET /api/soluciones/no-entregados?max={}", max);
        List<SolucionDTO> soluciones = solucionService.obtenerPorMaxNoEntregados(max);
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Obtener soluciones perfectas (0 no entregados)
     */
    @GetMapping("/perfectas")
    @Operation(summary = "Soluciones perfectas", description = "Obtiene soluciones sin paquetes no entregados")
    @ApiResponse(responseCode = "200", description = "Soluciones perfectas")
    public ResponseEntity<List<SolucionDTO>> obtenerPerfectas() {
        log.info("GET /api/soluciones/perfectas");
        List<SolucionDTO> soluciones = solucionService.obtenerSolucionesPerfectas();
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Obtener soluciones en un rango de fechas
     */
    @GetMapping("/fechas")
    @Operation(summary = "Soluciones por fechas", description = "Filtra soluciones creadas en un rango de fechas")
    @ApiResponse(responseCode = "200", description = "Soluciones en el rango de fechas")
    public ResponseEntity<List<SolucionDTO>> obtenerPorRangoFechas(
        @Parameter(description = "Fecha inicio (formato: yyyy-MM-dd'T'HH:mm:ss)") 
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
        @Parameter(description = "Fecha fin (formato: yyyy-MM-dd'T'HH:mm:ss)") 
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        log.info("GET /api/soluciones/fechas?inicio={}&fin={}", inicio, fin);
        List<SolucionDTO> soluciones = solucionService.obtenerPorRangoFechas(inicio, fin);
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Obtener últimas N soluciones
     */
    @GetMapping("/ultimas/{limite}")
    @Operation(summary = "Últimas soluciones", description = "Obtiene las últimas N soluciones creadas")
    @ApiResponse(responseCode = "200", description = "Últimas soluciones")
    public ResponseEntity<List<SolucionDTO>> obtenerUltimas(
        @Parameter(description = "Número de soluciones a retornar") @PathVariable int limite) {
        log.info("GET /api/soluciones/ultimas/{}", limite);
        List<SolucionDTO> soluciones = solucionService.obtenerUltimasSoluciones(limite);
        return ResponseEntity.ok(soluciones);
    }
    
    /**
     * Obtener estadísticas de soluciones
     */
    @GetMapping("/estadisticas")
    @Operation(summary = "Estadísticas", description = "Obtiene estadísticas agregadas de todas las soluciones")
    @ApiResponse(responseCode = "200", description = "Estadísticas de soluciones")
    public ResponseEntity<EstadisticasSolucionesDTO> obtenerEstadisticas() {
        log.info("GET /api/soluciones/estadisticas");
        EstadisticasSolucionesDTO stats = solucionService.obtenerEstadisticas();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Obtener estadísticas por algoritmo
     */
    @GetMapping("/estadisticas/{algoritmo}")
    @Operation(summary = "Estadísticas por algoritmo", description = "Obtiene estadísticas filtradas por algoritmo")
    @ApiResponse(responseCode = "200", description = "Estadísticas del algoritmo")
    public ResponseEntity<EstadisticasSolucionesDTO> obtenerEstadisticasPorAlgoritmo(
        @Parameter(description = "Nombre del algoritmo") @PathVariable String algoritmo) {
        log.info("GET /api/soluciones/estadisticas/{}", algoritmo);
        EstadisticasSolucionesDTO stats = solucionService.obtenerEstadisticasPorAlgoritmo(algoritmo);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Actualizar una solución
     */
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar solución", description = "Actualiza los datos de una solución existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solución actualizada"),
        @ApiResponse(responseCode = "404", description = "Solución no encontrada")
    })
    public ResponseEntity<Void> actualizarSolucion(
        @Parameter(description = "ID de la solución") @PathVariable Integer id,
        @RequestBody SolucionDTO solucionDTO) {
        log.info("PUT /api/soluciones/{} - Actualizar solución", id);
        try {
            solucionService.actualizarSolucion(id, solucionDTO);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error al actualizar solución: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Eliminar una solución
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar solución", description = "Elimina una solución y sus rutas asociadas")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Solución eliminada"),
        @ApiResponse(responseCode = "404", description = "Solución no encontrada")
    })
    public ResponseEntity<Void> eliminarSolucion(
        @Parameter(description = "ID de la solución") @PathVariable Integer id) {
        log.info("DELETE /api/soluciones/{}", id);
        try {
            solucionService.eliminarSolucion(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error al eliminar solución: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}

