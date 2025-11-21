package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.InstanciaVuelo;
import com.grupo5e.morapack.core.model.Vuelo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio que expande plantillas de vuelo (Vuelo) en instancias diarias (InstanciaVuelo)
 * para simulaciones multi-día.
 * 
 * Ejemplo:
 * - Vuelo LIM-CUZ sale diariamente a 20:00, llega 03:00 día siguiente
 * - Simulación: 2-9 Enero (7 días)
 * - Resultado: 7 InstanciaVuelo, una por día
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioExpansionVuelos {
    
    /**
     * Expande plantillas de vuelo en instancias diarias para la ventana de simulación.
     * 
     * @param vuelosTemplate Plantillas de vuelo base (desde DB o archivos)
     * @param horaInicioSim Inicio de ventana de simulación
     * @param horaFinSim Fin de ventana de simulación
     * @return Lista de instancias de vuelo cubriendo todos los días en la ventana
     */
    public List<InstanciaVuelo> expandirVuelosParaSimulacion(
            List<Vuelo> vuelosTemplate,
            LocalDateTime horaInicioSim,
            LocalDateTime horaFinSim) {
        
        List<InstanciaVuelo> instancias = new ArrayList<>();
        
        log.info("========================================");
        log.info("EXPANDIENDO VUELOS PARA SIMULACIÓN");
        log.info("Ventana: {} a {}", horaInicioSim, horaFinSim);
        log.info("Plantillas de vuelo: {}", vuelosTemplate.size());
        log.info("========================================");
        
        // Calcular número de días en la simulación
        long totalDias = ChronoUnit.DAYS.between(
            horaInicioSim.toLocalDate(),
            horaFinSim.toLocalDate()
        ) + 1; // Incluir día final
        
        log.info("La simulación abarca {} días", totalDias);
        
        // Para cada plantilla de vuelo, crear instancias para cada día
        for (Vuelo template : vuelosTemplate) {
            if (template.getHoraSalida() == null || template.getHoraLlegada() == null) {
                log.warn("ADVERTENCIA: Vuelo {} no tiene horarios de salida/llegada, omitiendo expansión",
                        template.getId());
                continue;
            }
            
            List<InstanciaVuelo> instanciasDiarias = expandirVueloIndividual(
                template,
                horaInicioSim,
                horaFinSim,
                (int) totalDias
            );
            
            instancias.addAll(instanciasDiarias);
        }
        
        log.info("Total de instancias de vuelo creadas: {}", instancias.size());
        log.info("========================================\n");
        
        return instancias;
    }
    
    /**
     * Expande un solo vuelo plantilla en instancias diarias.
     */
    private List<InstanciaVuelo> expandirVueloIndividual(
            Vuelo template,
            LocalDateTime horaInicioSim,
            LocalDateTime horaFinSim,
            int totalDias) {
        
        List<InstanciaVuelo> instancias = new ArrayList<>();
        LocalDate fechaInicio = horaInicioSim.toLocalDate();
        
        for (int dia = 0; dia < totalDias; dia++) {
            LocalDate fechaActual = fechaInicio.plusDays(dia);
            
            // Crear datetime de salida combinando fecha + hora
            LocalDateTime fechaHoraSalida = LocalDateTime.of(
                fechaActual,
                template.getHoraSalida()
            );
            
            // Solo crear instancia si la salida está dentro de la ventana de simulación
            if (fechaHoraSalida.isBefore(horaInicioSim) ||
                fechaHoraSalida.isAfter(horaFinSim)) {
                continue; // Saltar esta instancia
            }
            
            // Calcular datetime de llegada
            LocalDateTime fechaHoraLlegada = calcularFechaHoraLlegada(
                fechaHoraSalida,
                template.getHoraSalida(),
                template.getHoraLlegada(),
                template.getTiempoTransporte()
            );
            
            // Crear instancia de vuelo
            InstanciaVuelo instancia = InstanciaVuelo.builder()
                .vueloBase(template)
                .fechaHoraSalida(fechaHoraSalida)
                .fechaHoraLlegada(fechaHoraLlegada)
                .diaInstancia(dia)
                .capacidadMaxima(template.getCapacidadMaxima())
                .capacidadUsada(0) // Inicialmente vacío
                .build();
            
            // Generar ID único de instancia
            instancia.generarIdInstancia();
            
            instancias.add(instancia);
        }
        
        return instancias;
    }
    
    /**
     * Calcula la fecha/hora de llegada manejando vuelos que cruzan medianoche.
     * 
     * Ejemplos:
     * - Sale 20:00, Llega 03:00 → día siguiente a las 03:00
     * - Sale 08:00, Llega 12:00 → mismo día a las 12:00
     */
    private LocalDateTime calcularFechaHoraLlegada(
            LocalDateTime fechaHoraSalida,
            LocalTime horaSalida,
            LocalTime horaLlegada,
            Double tiempoTransporteDias) {
        
        // Método 1: Usar horarios programados (preferido si están disponibles)
        if (horaSalida != null && horaLlegada != null) {
            LocalDateTime llegadaMismoDia = LocalDateTime.of(
                fechaHoraSalida.toLocalDate(),
                horaLlegada
            );
            
            // Si hora de llegada es antes que hora de salida, el vuelo cruza medianoche
            if (horaLlegada.isBefore(horaSalida)) {
                return llegadaMismoDia.plusDays(1);
            } else {
                return llegadaMismoDia;
            }
        }
        
        // Método 2: Fallback a tiempo de transporte (si horario no disponible)
        if (tiempoTransporteDias != null) {
            long minutosTransporte = (long) (tiempoTransporteDias * 24 * 60);
            return fechaHoraSalida.plusMinutes(minutosTransporte);
        }
        
        // Método 3: Último recurso - asumir mismo día
        log.warn("ADVERTENCIA: No hay horario ni tiempo de transporte disponible para cálculo de vuelo");
        return fechaHoraSalida.plusHours(1); // Vuelo de 1 hora por defecto
    }
    
    /**
     * Crea un mapa de búsqueda para recuperación rápida de instancias.
     * Clave: "codigoOrigen-codigoDest-YYYY-MM-DD-HH:mm"
     * 
     * Ejemplo: "LIM-CUZ-2025-01-02-20:00"
     */
    public Map<String, InstanciaVuelo> crearMapaBusquedaInstancias(
            List<InstanciaVuelo> instancias) {
        
        Map<String, InstanciaVuelo> mapaBusqueda = new HashMap<>();
        
        for (InstanciaVuelo instancia : instancias) {
            String clave = construirClaveBusqueda(instancia);
            mapaBusqueda.put(clave, instancia);
        }
        
        log.info("Creado mapa de búsqueda de instancias con {} entradas", mapaBusqueda.size());
        return mapaBusqueda;
    }
    
    /**
     * Construye una clave de búsqueda para una instancia de vuelo.
     */
    private String construirClaveBusqueda(InstanciaVuelo instancia) {
        Vuelo vueloBase = instancia.getVueloBase();
        LocalDateTime salida = instancia.getFechaHoraSalida();
        
        return String.format("%s-%s-%s-%02d:%02d",
            vueloBase.getAeropuertoOrigen().getCodigoIATA(),
            vueloBase.getAeropuertoDestino().getCodigoIATA(),
            salida.toLocalDate(),
            salida.getHour(),
            salida.getMinute()
        );
    }
    
    /**
     * Filtra instancias que salen dentro de una ventana de tiempo específica.
     * Útil para ejecuciones incrementales del algoritmo.
     */
    public List<InstanciaVuelo> filtrarPorVentanaSalida(
            List<InstanciaVuelo> instancias,
            LocalDateTime ventanaInicio,
            LocalDateTime ventanaFin) {
        
        return instancias.stream()
            .filter(inst -> {
                LocalDateTime salida = inst.getFechaHoraSalida();
                return !salida.isBefore(ventanaInicio) && !salida.isAfter(ventanaFin);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Agrupa instancias por ID de vuelo base.
     * Útil para analizar repeticiones diarias.
     */
    public Map<Integer, List<InstanciaVuelo>> agruparPorVueloBase(
            List<InstanciaVuelo> instancias) {
        
        return instancias.stream()
            .collect(Collectors.groupingBy(
                inst -> inst.getVueloBase().getId()
            ));
    }
    
    /**
     * Imprime resumen de expansión para debugging.
     */
    public void imprimirResumenExpansion(List<InstanciaVuelo> instancias) {
        log.info("\n=== RESUMEN DE EXPANSIÓN DE VUELOS ===");
        
        Map<Integer, List<InstanciaVuelo>> agrupadas = agruparPorVueloBase(instancias);
        
        for (Map.Entry<Integer, List<InstanciaVuelo>> entrada : agrupadas.entrySet()) {
            List<InstanciaVuelo> instanciasDiarias = entrada.getValue();
            if (!instanciasDiarias.isEmpty()) {
                InstanciaVuelo primera = instanciasDiarias.get(0);
                log.info("{}: {} instancias diarias creadas",
                    primera.getVueloBase().getIdentificadorVuelo(),
                    instanciasDiarias.size()
                );
            }
        }
        
        log.info("Total de instancias: {}", instancias.size());
        log.info("=================================\n");
    }
    
    /**
     * Encuentra la próxima instancia disponible de un vuelo con capacidad suficiente.
     * 
     * @param vueloBase Vuelo base
     * @param instancias Todas las instancias disponibles
     * @param tiempoActual Tiempo actual de simulación
     * @param cantidadRequerida Cantidad de productos a asignar
     * @return Primera instancia con capacidad suficiente, o null si no hay
     */
    public InstanciaVuelo encontrarProximaInstanciaDisponible(
            Vuelo vueloBase,
            List<InstanciaVuelo> instancias,
            LocalDateTime tiempoActual,
            int cantidadRequerida) {
        
        return instancias.stream()
            .filter(inst -> inst.getVueloBase().getId().equals(vueloBase.getId()))
            .filter(inst -> !inst.getFechaHoraSalida().isBefore(tiempoActual))
            .filter(inst -> inst.tieneCapacidad(cantidadRequerida))
            .min(Comparator.comparing(InstanciaVuelo::getFechaHoraSalida))
            .orElse(null);
    }
}

