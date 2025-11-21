package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.enums.EstadoVuelo;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.repository.ProductoRepository;
import com.grupo5e.morapack.repository.VueloRepository;
import com.grupo5e.morapack.service.VueloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VueloServiceImpl implements VueloService {

    private final VueloRepository vueloRepository;
    private final ProductoRepository productoRepository;

    public VueloServiceImpl(VueloRepository vueloRepository, ProductoRepository productoRepository) {
        this.vueloRepository = vueloRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    public List<Vuelo> listar() {
        return vueloRepository.findAll();
    }

    @Override
    @Transactional
    public Integer insertar(Vuelo vuelo) {
        return vueloRepository.save(vuelo).getId();
    }

    @Override
    @Transactional
    public Vuelo actualizar(Integer id, Vuelo vuelo) {
        Vuelo existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Vuelo", "id", id);
        }
        vuelo.setId(id);
        return vueloRepository.save(vuelo);
    }

    @Override
    public Vuelo buscarPorId(Integer id) {
        return vueloRepository.findById(id).orElse(null);
    }

    @Override
    public List<Vuelo> buscarPorRuta(Integer origenId, Integer destinoId) {
        return vueloRepository.findByAeropuertoOrigenIdAndAeropuertoDestinoId(origenId, destinoId);
    }

    @Override
    public List<Vuelo> buscarPorEstado(EstadoVuelo estado) {
        return vueloRepository.findByEstado(estado);
    }

    @Override
    public List<Vuelo> buscarDisponibles(Integer capacidadMinima) {
        return vueloRepository.findByCapacidadMaximaGreaterThanEqual(capacidadMinima);
    }

    @Override
    public Optional<Vuelo> buscarPorIdentificador(String identificador) {
        // Implementación básica, puede mejorarse con query personalizada
        return vueloRepository.findAll().stream()
                .filter(v -> identificador.equals(v.getIdentificadorVuelo()))
                .findFirst();
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Vuelo", "id", id);
        }
        vueloRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Integer id) {
        return vueloRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Vuelo> insertarBulk(List<Vuelo> vuelos) {
        return vueloRepository.saveAll(vuelos).stream().collect(Collectors.toList());
    }
    
    /**
     * Valida si un vuelo puede ser cancelado (no ha despegado aún).
     * 
     * REGLAS DE VALIDACIÓN:
     * 1. El vuelo debe existir
     * 2. NO debe tener productos en estado IN_TRANSIT (ya despegó)
     * 3. El tiempo actual debe ser anterior a la próxima hora de salida
     * 
     * @param vueloId ID del vuelo
     * @param tiempoSimulacionActual Tiempo actual de la simulación
     * @return true si el vuelo puede cancelarse, false si ya despegó
     * @throws IllegalStateException si el vuelo no existe
     */
    @Override
    @Transactional(readOnly = true)
    public boolean puedeSerCancelado(Integer vueloId, LocalDateTime tiempoSimulacionActual) {
        log.debug("Validando si vuelo {} puede ser cancelado a tiempo {}", vueloId, tiempoSimulacionActual);
        
        // 1. Verificar que el vuelo existe
        Vuelo vuelo = vueloRepository.findById(vueloId)
            .orElseThrow(() -> new IllegalStateException("Vuelo no encontrado: " + vueloId));
        
        // 2. Obtener productos asignados a este vuelo
        // Buscar por instancias que contengan el ID del vuelo (formato "FL-{vueloId}-...")
        String patronBusqueda = "FL-" + vueloId + "-";
        List<Producto> productosAsignados = productoRepository
            .findByInstanciaVueloAsignadaContaining(patronBusqueda);
        
        log.debug("Vuelo {} tiene {} productos asignados", vueloId, productosAsignados.size());
        
        // 3. Verificar si hay productos en tránsito (vuelo ya despegó)
        boolean hayProductosEnTransito = productosAsignados.stream()
            .anyMatch(p -> p.getEstado() != null && 
                          p.getEstado().name().equals("IN_TRANSIT"));
        
        if (hayProductosEnTransito) {
            log.warn("Vuelo {} no puede cancelarse - tiene productos en tránsito", vueloId);
            return false;
        }
        
        // 4. Verificar horario de salida vs tiempo simulación
        // Si hay productos asignados, verificar la próxima instancia programada
        if (!productosAsignados.isEmpty()) {
            LocalDateTime proximaSalida = obtenerProximaHoraSalida(vuelo, productosAsignados);
            
            if (proximaSalida != null && !tiempoSimulacionActual.isBefore(proximaSalida)) {
                log.warn("Vuelo {} no puede cancelarse - próxima salida {} ya ocurrió (tiempo actual: {})", 
                         vueloId, proximaSalida, tiempoSimulacionActual);
                return false;
            }
        }
        
        log.info("Vuelo {} puede ser cancelado", vueloId);
        return true;
    }
    
    /**
     * Obtiene la próxima hora de salida de un vuelo basándose en sus instancias asignadas.
     * 
     * @param vuelo Vuelo
     * @param productos Productos asignados al vuelo
     * @return Próxima hora de salida, o null si no se puede determinar
     */
    private LocalDateTime obtenerProximaHoraSalida(Vuelo vuelo, List<Producto> productos) {
        // Obtener la salida más temprana de todas las instancias asignadas
        return productos.stream()
            .map(p -> {
                String instancia = p.getInstanciaVueloAsignada();
                return parsearFechaSalidaDeInstancia(instancia);
            })
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(null);
    }
    
    /**
     * Parsea la fecha/hora de salida desde un ID de instancia de vuelo.
     * Formato: "FL-{vueloId}-DAY-{day}-{HHmm}"
     * Ejemplo: "FL-45-DAY-1-0800" -> día 1, hora 08:00
     * 
     * NOTA: Este método tiene limitaciones porque no tenemos la fecha base de simulación
     * en este contexto. En producción, sería mejor almacenar fechaHoraSalida directamente
     * en el modelo Producto o consultar la tabla instancias_vuelo.
     * 
     * @param idInstancia ID de instancia de vuelo
     * @return Fecha/hora de salida parseada, o null si no se puede parsear
     */
    private LocalDateTime parsearFechaSalidaDeInstancia(String idInstancia) {
        if (idInstancia == null || !idInstancia.startsWith("FL-")) {
            return null;
        }
        
        try {
            // "FL-45-DAY-1-0800" -> split por "-" -> ["FL", "45", "DAY", "1", "0800"]
            String[] partes = idInstancia.split("-");
            if (partes.length < 5) {
                return null;
            }
            
            int dia = Integer.parseInt(partes[3]);
            String horaStr = partes[4];
            int hora = Integer.parseInt(horaStr.substring(0, 2));
            int minuto = horaStr.length() >= 4 ? 
                Integer.parseInt(horaStr.substring(2, 4)) : 0;
            
            // LIMITACIÓN: No tenemos fecha base de simulación aquí
            // Por ahora, retornar null y confiar en validación por estado IN_TRANSIT
            // En producción, deberíamos:
            // 1. Almacenar fechaHoraSalida en Producto
            // 2. O consultar tabla instancias_vuelo por ID
            return null;
            
        } catch (Exception e) {
            log.debug("No se pudo parsear fecha de instancia: {}", idInstancia, e);
            return null;
        }
    }
}
