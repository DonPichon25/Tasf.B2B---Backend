package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.dto.EstadisticasPlanesDTO;
import com.grupo5e.morapack.api.dto.PlanViajeDTO;
import com.grupo5e.morapack.api.dto.SegmentoVueloDTO;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.PlanViaje;
import com.grupo5e.morapack.core.model.SegmentoVuelo;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.repository.PedidoRepository;
import com.grupo5e.morapack.repository.PlanViajeRepository;
import com.grupo5e.morapack.repository.SegmentoVueloRepository;
import com.grupo5e.morapack.repository.VueloRepository;
import com.grupo5e.morapack.service.PlanViajeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de Plan de Viaje
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanViajeServiceImpl implements PlanViajeService {
    
    private final PlanViajeRepository planViajeRepository;
    private final SegmentoVueloRepository segmentoVueloRepository;
    private final PedidoRepository pedidoRepository;
    private final VueloRepository vueloRepository;
    
    @Override
    @Transactional
    public Integer crearPlanViaje(PlanViajeDTO planViajeDTO) {
        log.info("Creando plan de viaje para pedido {}", planViajeDTO.getPedidoId());
        
        // Buscar pedido
        Pedido pedido = pedidoRepository.findById(planViajeDTO.getPedidoId())
            .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + planViajeDTO.getPedidoId()));
        
        // Crear entidad PlanViaje
        PlanViaje planViaje = PlanViaje.builder()
            .fechaPlanificacion(planViajeDTO.getFechaPlanificacion())
            .estado(planViajeDTO.getEstado() != null ? planViajeDTO.getEstado() : "PENDIENTE")
            .algoritmoUsado(planViajeDTO.getAlgoritmoUsado() != null ? planViajeDTO.getAlgoritmoUsado() : "ALNS")
            .versionDatos(planViajeDTO.getVersionDatos())
            .costoTotal(planViajeDTO.getCostoTotal())
            .tiempoTotalHoras(planViajeDTO.getTiempoTotalHoras())
            .numeroVuelos(planViajeDTO.getNumeroVuelos())
            .pedido(pedido)
            .segmentosVuelo(new ArrayList<>())
            .build();
        
        // Guardar plan primero para obtener ID
        planViaje = planViajeRepository.save(planViaje);
        
        // Crear segmentos si existen
        if (planViajeDTO.getSegmentosVuelo() != null && !planViajeDTO.getSegmentosVuelo().isEmpty()) {
            for (SegmentoVueloDTO segmentoDTO : planViajeDTO.getSegmentosVuelo()) {
                SegmentoVuelo segmento = convertirDTOASegmento(segmentoDTO, planViaje, pedido);
                planViaje.agregarSegmento(segmento);
            }
            // Actualizar con segmentos
            planViaje = planViajeRepository.save(planViaje);
        }
        
        log.info("Plan de viaje creado exitosamente con ID: {}", planViaje.getId());
        return planViaje.getId();
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlanViajeDTO obtenerPorId(Integer id) {
        PlanViaje plan = planViajeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Plan de viaje no encontrado: " + id));
        return convertirADTO(plan);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlanViajeDTO> obtenerTodos() {
        return planViajeRepository.findAll().stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlanViajeDTO> obtenerPorPedidoId(Integer pedidoId) {
        return planViajeRepository.findByPedidoId(pedidoId).stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PlanViajeDTO> obtenerPorEstado(String estado) {
        return planViajeRepository.findByEstado(estado).stream()
            .map(this::convertirADTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public PlanViajeDTO obtenerPlanMasRecientePorPedido(Integer pedidoId) {
        List<PlanViaje> planes = planViajeRepository.findMostRecentByPedidoId(pedidoId);
        if (planes.isEmpty()) {
            throw new RuntimeException("No se encontraron planes para el pedido: " + pedidoId);
        }
        return convertirADTO(planes.get(0));
    }
    
    @Override
    @Transactional
    public void actualizarEstado(Integer planId, String nuevoEstado) {
        log.info("Actualizando estado del plan {} a {}", planId, nuevoEstado);
        PlanViaje plan = planViajeRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("Plan de viaje no encontrado: " + planId));
        plan.setEstado(nuevoEstado);
        planViajeRepository.save(plan);
    }
    
    @Override
    @Transactional
    public void eliminar(Integer id) {
        log.info("Eliminando plan de viaje {}", id);
        planViajeRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SegmentoVueloDTO> obtenerSegmentosPorPlan(Integer planId) {
        return segmentoVueloRepository.findByPlanViajeIdOrderByOrdenSegmentoAsc(planId).stream()
            .map(this::convertirSegmentoADTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public EstadisticasPlanesDTO obtenerEstadisticas() {
        List<PlanViaje> todosLosPlanes = planViajeRepository.findAll();
        
        long totalPlanes = todosLosPlanes.size();
        long pendientes = planViajeRepository.countByEstado("PENDIENTE");
        long enProgreso = planViajeRepository.countByEstado("EN_PROGRESO");
        long completados = planViajeRepository.countByEstado("COMPLETADO");
        long cancelados = planViajeRepository.countByEstado("CANCELADO");
        
        double costoPromedio = todosLosPlanes.stream()
            .filter(p -> p.getCostoTotal() != null)
            .mapToDouble(PlanViaje::getCostoTotal)
            .average()
            .orElse(0.0);
        
        double tiempoPromedio = todosLosPlanes.stream()
            .filter(p -> p.getTiempoTotalHoras() != null)
            .mapToDouble(PlanViaje::getTiempoTotalHoras)
            .average()
            .orElse(0.0);
        
        double promedioVuelos = todosLosPlanes.stream()
            .filter(p -> p.getNumeroVuelos() != null)
            .mapToDouble(PlanViaje::getNumeroVuelos)
            .average()
            .orElse(0.0);
        
        return EstadisticasPlanesDTO.builder()
            .totalPlanes(totalPlanes)
            .planesPendientes(pendientes)
            .planesEnProgreso(enProgreso)
            .planesCompletados(completados)
            .planesCancelados(cancelados)
            .costoPromedio(costoPromedio)
            .tiempoPromedioHoras(tiempoPromedio)
            .promedioVuelosPorPlan(promedioVuelos)
            .build();
    }
    
    /**
     * Convierte entidad a DTO
     */
    private PlanViajeDTO convertirADTO(PlanViaje plan) {
        List<SegmentoVueloDTO> segmentos = plan.getSegmentosVuelo().stream()
            .map(this::convertirSegmentoADTO)
            .collect(Collectors.toList());
        
        return PlanViajeDTO.builder()
            .id(plan.getId())
            .fechaPlanificacion(plan.getFechaPlanificacion())
            .estado(plan.getEstado())
            .algoritmoUsado(plan.getAlgoritmoUsado())
            .versionDatos(plan.getVersionDatos())
            .costoTotal(plan.getCostoTotal())
            .tiempoTotalHoras(plan.getTiempoTotalHoras())
            .numeroVuelos(plan.getNumeroVuelos())
            .pedidoId(plan.getPedido().getId())
            .segmentosVuelo(segmentos)
            .createdAt(plan.getCreatedAt())
            .updatedAt(plan.getUpdatedAt())
            .build();
    }
    
    /**
     * Convierte SegmentoVuelo a DTO
     */
    private SegmentoVueloDTO convertirSegmentoADTO(SegmentoVuelo segmento) {
        return SegmentoVueloDTO.builder()
            .id(segmento.getId())
            .ordenSegmento(segmento.getOrdenSegmento())
            .horaSalidaEstimada(segmento.getHoraSalidaEstimada())
            .horaLlegadaEstimada(segmento.getHoraLlegadaEstimada())
            .capacidadReservada(segmento.getCapacidadReservada())
            .codigoOrigen(segmento.getCodigoOrigen())
            .codigoDestino(segmento.getCodigoDestino())
            .duracionHoras(segmento.getDuracionHoras())
            .planViajeId(segmento.getPlanViaje().getId())
            .vueloId(segmento.getVuelo() != null ? segmento.getVuelo().getId() : null)
            .pedidoId(segmento.getPedido() != null ? segmento.getPedido().getId() : null)
            .createdAt(segmento.getCreatedAt())
            .build();
    }
    
    /**
     * Convierte DTO a SegmentoVuelo
     */
    private SegmentoVuelo convertirDTOASegmento(SegmentoVueloDTO dto, PlanViaje plan, Pedido pedido) {
        // Buscar vuelo si se proporcionó ID
        Vuelo vuelo = null;
        if (dto.getVueloId() != null) {
            vuelo = vueloRepository.findById(dto.getVueloId()).orElse(null);
        }
        
        return SegmentoVuelo.builder()
            .ordenSegmento(dto.getOrdenSegmento())
            .horaSalidaEstimada(dto.getHoraSalidaEstimada())
            .horaLlegadaEstimada(dto.getHoraLlegadaEstimada())
            .capacidadReservada(dto.getCapacidadReservada())
            .codigoOrigen(dto.getCodigoOrigen())
            .codigoDestino(dto.getCodigoDestino())
            .duracionHoras(dto.getDuracionHoras())
            .planViaje(plan)
            .vuelo(vuelo)
            .pedido(pedido)
            .build();
    }
}

