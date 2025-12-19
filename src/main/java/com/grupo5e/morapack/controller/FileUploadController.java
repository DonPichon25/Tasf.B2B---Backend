package com.grupo5e.morapack.controller;

import com.grupo5e.morapack.api.dto.FileUploadValidationResponse;
import com.grupo5e.morapack.api.dto.FileValidationResult;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.service.FileParsingService;
import com.grupo5e.morapack.service.TemporaryDataStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Controlador para carga y validación de archivos de simulación
 */
@RestController
@RequestMapping("/api/simulacion/upload")
@Tag(name = "Carga de Archivos", description = "API para cargar y validar archivos de datos para simulación")
@Slf4j
@CrossOrigin(origins = "*")
public class FileUploadController {
    
    private final FileParsingService fileParsingService;
    private final TemporaryDataStorageService temporaryDataStorageService;
    
    // Tamaño máximo: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    public FileUploadController(FileParsingService fileParsingService,
                               TemporaryDataStorageService temporaryDataStorageService) {
        this.fileParsingService = fileParsingService;
        this.temporaryDataStorageService = temporaryDataStorageService;
    }
    
    @Operation(
            summary = "Validar archivos de simulación",
            description = "Valida y parsea archivos de aeropuertos, vuelos y pedidos. " +
                         "Todos los archivos son opcionales. Si no se proporciona un archivo, " +
                         "se usarán los datos existentes en la base de datos para ese tipo."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Archivos validados exitosamente",
                    content = @Content(schema = @Schema(implementation = FileUploadValidationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error de validación en los archivos"
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "Archivo demasiado grande (máx 10MB)"
            )
    })
    @PostMapping("/validate")
    public ResponseEntity<FileUploadValidationResponse> validateFiles(
            @Parameter(description = "Archivo aeropuertosinfo.txt (opcional)")
            @RequestParam(value = "aeropuertos", required = false) MultipartFile aeropuertosFile,
            
            @Parameter(description = "Archivo vuelos.txt (opcional)")
            @RequestParam(value = "vuelos", required = false) MultipartFile vuelosFile,
            
            @Parameter(description = "Archivo pedidos.txt (opcional)")
            @RequestParam(value = "pedidos", required = false) MultipartFile pedidosFile
    ) {
        log.info("📤 Recibida solicitud de validación de archivos");
        log.info("   - Aeropuertos: {}", aeropuertosFile != null ? aeropuertosFile.getOriginalFilename() : "no proporcionado");
        log.info("   - Vuelos: {}", vuelosFile != null ? vuelosFile.getOriginalFilename() : "no proporcionado");
        log.info("   - Pedidos: {}", pedidosFile != null ? pedidosFile.getOriginalFilename() : "no proporcionado");
        
        FileUploadValidationResponse response = FileUploadValidationResponse.builder()
                .success(true)
                .usingDatabaseFallback(false)
                .build();
        
        // Si no se proporcionó ningún archivo
        if (aeropuertosFile == null && vuelosFile == null && pedidosFile == null) {
            response.setSuccess(true);
            response.setMessage("No se proporcionaron archivos. Se usarán los datos de la base de datos.");
            response.setUsingDatabaseFallback(true);
            return ResponseEntity.ok(response);
        }
        
        // Validar tamaño de archivos
        if (!validateFileSize(aeropuertosFile, response) ||
            !validateFileSize(vuelosFile, response) ||
            !validateFileSize(pedidosFile, response)) {
            response.setSuccess(false);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
        }
        
        try {
            // Validar y parsear archivos
            FileValidationResult aeropuertosResult = null;
            FileValidationResult vuelosResult = null;
            FileValidationResult pedidosResult = null;
            
            // Paso 1: Parsear aeropuertos primero (si existe)
            if (aeropuertosFile != null) {
                log.info("🏢 Validando aeropuertos...");
                aeropuertosResult = fileParsingService.validateAndParseAeropuertos(aeropuertosFile.getBytes());
                response.setAeropuertos(aeropuertosResult);
                
                if (!aeropuertosResult.isSuccess()) {
                    log.error("❌ Error al validar aeropuertos, no se procesarán vuelos ni pedidos");
                }
            }
            
            // Paso 2: Parsear vuelos usando los aeropuertos del paso 1
            if (vuelosFile != null) {
                log.info("✈️ Validando vuelos...");
                List<Aeropuerto> aeropuertosParseados = 
                    (aeropuertosResult != null && aeropuertosResult.getParsedAeropuertos() != null) 
                        ? aeropuertosResult.getParsedAeropuertos() 
                        : null;
                
                vuelosResult = fileParsingService.validateAndParseVuelos(vuelosFile.getBytes(), aeropuertosParseados);
                response.setVuelos(vuelosResult);
            }
            
            // Paso 3: Parsear pedidos usando los aeropuertos del paso 1
            if (pedidosFile != null) {
                log.info("📦 Validando pedidos...");
                List<Aeropuerto> aeropuertosParseados = 
                    (aeropuertosResult != null && aeropuertosResult.getParsedAeropuertos() != null) 
                        ? aeropuertosResult.getParsedAeropuertos() 
                        : null;
                
                pedidosResult = fileParsingService.validateAndParsePedidos(pedidosFile.getBytes(), aeropuertosParseados);
                response.setPedidos(pedidosResult);
            }
            
            // Determinar si hubo errores
            boolean hasErrors = (aeropuertosResult != null && !aeropuertosResult.isSuccess()) ||
                               (vuelosResult != null && !vuelosResult.isSuccess()) ||
                               (pedidosResult != null && !pedidosResult.isSuccess());
            
            response.setSuccess(!hasErrors);
            
            if (hasErrors) {
                response.setMessage("Se encontraron errores en la validación. Revisa los detalles de cada archivo.");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Generar session ID y almacenar datos temporales
            String sessionId = UUID.randomUUID().toString();
            response.setSessionId(sessionId);
            
            // Almacenar datos parseados en memoria
            temporaryDataStorageService.storeTemporaryData(
                    sessionId,
                    aeropuertosResult,
                    vuelosResult,
                    pedidosResult
            );
            
            // Mensaje de éxito
            int filesUploaded = (aeropuertosFile != null ? 1 : 0) + 
                               (vuelosFile != null ? 1 : 0) + 
                               (pedidosFile != null ? 1 : 0);
            
            boolean needsDbFallback = aeropuertosFile == null || vuelosFile == null || pedidosFile == null;
            response.setUsingDatabaseFallback(needsDbFallback);
            
            response.setMessage(String.format(
                    "✅ Validación exitosa. %d archivo(s) procesado(s).%s",
                    filesUploaded,
                    needsDbFallback ? " Los datos faltantes se obtendrán de la base de datos." : ""
            ));
            
            log.info("✅ Validación exitosa. Session ID: {}", sessionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error durante la validación de archivos", e);
            response.setSuccess(false);
            response.setMessage("Error interno durante la validación: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private boolean validateFileSize(MultipartFile file, FileUploadValidationResponse response) {
        if (file != null && file.getSize() > MAX_FILE_SIZE) {
            response.setSuccess(false);
            response.setMessage(String.format(
                    "Archivo %s es demasiado grande (%.2f MB). Tamaño máximo: 10 MB",
                    file.getOriginalFilename(),
                    file.getSize() / (1024.0 * 1024.0)
            ));
            return false;
        }
        return true;
    }

    @Operation(summary = "Validación volátil para Test de Colapso",
            description = "Procesa pedidos y cancelaciones en RAM sin persistir en BD.")
    @PostMapping("/validar-volatil")
    public ResponseEntity<FileUploadValidationResponse> validarVolatil(
            @Parameter(description = "Lista de archivos de pedidos (.txt)")
            @RequestParam("files") List<MultipartFile> files,

            @Parameter(description = "Archivo de cancelaciones (opcional)")
            @RequestParam(value = "cancelaciones", required = false) MultipartFile cancelacionesFile) {

        log.info("🚀 Iniciando validación volátil para colapso. Archivos recibidos: {}", files.size());
        FileUploadValidationResponse response = new FileUploadValidationResponse();
        response.setSuccess(true);
        String sessionId = UUID.randomUUID().toString();
        response.setSessionId(sessionId);

        try {
            // 1. Validar tamaño de archivos
            for (MultipartFile file : files) {
                if (!validateFileSize(file, response)) return ResponseEntity.badRequest().body(response);
            }
            if (cancelacionesFile != null && !validateFileSize(cancelacionesFile, response)) {
                return ResponseEntity.badRequest().body(response);
            }

            // 2. Procesar Pedidos (107k o los que vengan)
            // Asumimos que parseOrders devuelve la lista de objetos pero NO guarda en BD
            List<Pedido> todosLosPedidos = new java.util.ArrayList<>();
            for (MultipartFile file : files) {
                List<Pedido> pedidosParciales = fileParsingService.parseOrders(file);
                todosLosPedidos.addAll(pedidosParciales);

                response.getDetails().add(FileValidationResult.builder()
                        .fileName(file.getOriginalFilename())
                        .success(true)
                        .parsedCount(pedidosParciales.size())
                        .build());
            }

            // 3. Procesar Cancelaciones si existen
            if (cancelacionesFile != null) {
                // Aquí podrías tener un parseCancellations similar
                // List<Cancelacion> cancelaciones = fileParsingService.parseCancellations(cancelacionesFile);
                // temporaryDataStorageService.storeCancellations(sessionId, cancelaciones);

                response.getDetails().add(FileValidationResult.builder()
                        .fileName(cancelacionesFile.getOriginalFilename())
                        .success(true)
                        .parsedCount(1) // O el conteo real
                        .build());
            }

            // 4. Guardar en Memoria Temporal (RAM)
            // Este servicio es el que usará el algoritmo de colapso después
            temporaryDataStorageService.storeOrders(sessionId, todosLosPedidos);

            response.setMessage(String.format(
                    "✅ Validación volátil exitosa. %d pedidos listos en RAM para el test de colapso.",
                    todosLosPedidos.size()
            ));

            log.info("✅ Datos volátiles listos. Session ID: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error en validación volátil", e);
            response.setSuccess(false);
            response.setMessage("Error en el procesamiento RAM: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

