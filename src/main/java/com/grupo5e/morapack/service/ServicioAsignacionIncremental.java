package com.grupo5e.morapack.service;

import com.grupo5e.morapack.api.dto.ResultadoAsignacionIncrementalDTO;
import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.InstanciaVuelo;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.repository.InstanciaVueloRepository;
import com.grupo5e.morapack.repository.ProductoRepository;
import com.grupo5e.morapack.service.impl.AeropuertoServiceImpl;
import com.grupo5e.morapack.service.impl.PedidoServiceImpl;
import com.grupo5e.morapack.service.impl.VueloServiceImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para asignación incremental de pedidos sin re-optimizar todo el plan.
 * 
 * ESTRATEGIA:
 * 1. Cargar capacidades actuales de vuelos (usando productos ya asignados)
 * 2. Buscar espacio disponible en vuelos existentes
 * 3. Asignar el pedido si hay capacidad
 * 4. Si no hay capacidad, indicar que se requiere re-optimización
 * 
 * Este servicio permite agregar pedidos "en caliente" durante una simulación
 * sin pausar o re-ejecutar el algoritmo completo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicioAsignacionIncremental {
    
    private final VueloServiceImpl vueloService;
    private final ProductoRepository productoRepository;
    private final PedidoServiceImpl pedidoService;
    private final AeropuertoServiceImpl aeropuertoService;
    private final ServicioExpansionVuelos servicioExpansion;
    private final InstanciaVueloRepository instanciaVueloRepository;
    
    /**
     * Asigna un pedido nuevo sin re-optimizar todo el plan.
     * Usa capacidades actuales (PREFILL) y busca espacio disponible.
     * 
     * @param pedidoId ID del pedido a asignar
     * @param tiempoActual Tiempo actual de simulación
     * @return Resultado de la asignación incremental
     */
    @Transactional
    public ResultadoAsignacionIncrementalDTO asignarPedidoIncremental(
            Integer pedidoId,
            LocalDateTime tiempoActual) {
        
        long startTime = System.currentTimeMillis();
        
        log.info("========================================");
        log.info("ASIGNACIÓN INCREMENTAL - Pedido {}", pedidoId);
        log.info("Tiempo actual: {}", tiempoActual);
        log.info("========================================");
        
        // 1. Obtener el pedido
        Pedido pedido = pedidoService.buscarPorId(pedidoId);
        if (pedido == null) {
            log.error("Pedido {} no encontrado", pedidoId);
            return ResultadoAsignacionIncrementalDTO.builder()
                .exitoso(false)
                .pedidoId(pedidoId)
                .mensaje("Pedido no encontrado")
                .build();
        }
        
        log.info("Pedido: {} unidades, origen: {}, destino: {}",
            pedido.getCantidadProductosRapido(),
            pedido.getAeropuertoOrigenCodigo(),
            pedido.getAeropuertoDestinoCodigo()
        );
        
        // 2. Cargar estado actual de capacidades (PREFILL)
        Map<String, Integer> capacidadesActuales = cargarCapacidadesActuales();
        log.info("✓ Capacidades actuales cargadas: {} instancias de vuelo con productos",
            capacidadesActuales.size());
        
        // 3. Obtener vuelos disponibles
        List<Vuelo> vuelosDisponibles = vueloService.listar();
        log.info("✓ Vuelos disponibles: {}", vuelosDisponibles.size());
        
        // 4. Buscar ruta con espacio disponible
        RutaEncontrada ruta = buscarRutaConEspacioDisponible(
            pedido,
            vuelosDisponibles,
            capacidadesActuales,
            tiempoActual
        );
        
        if (ruta == null) {
            log.warn("✗ No se encontró espacio disponible para pedido {}", pedidoId);
            long duration = System.currentTimeMillis() - startTime;
            return ResultadoAsignacionIncrementalDTO.builder()
                .exitoso(false)
                .pedidoId(pedidoId)
                .mensaje("No hay capacidad disponible - se requiere re-optimización completa")
                .tiempoEjecucionMs(duration)
                .build();
        }
        
        log.info("✓ Ruta encontrada: {} vuelos", ruta.getVuelos().size());
        ruta.getVuelos().forEach(v -> 
            log.info("  - {}", v.getIdentificadorVuelo())
        );
        
        // 5. Crear y asignar productos a la ruta encontrada
        List<Producto> productosCreados = crearYAsignarProductos(
            pedido,
            ruta.getVuelos(),
            ruta.getInstancias()
        );
        
        log.info("✓ Productos creados y asignados: {}", productosCreados.size());
        
        // 6. Actualizar capacidades de vuelos
        for (InstanciaVuelo instancia : ruta.getInstancias()) {
            Vuelo vuelo = instancia.getVueloBase();
            int nuevaCapacidadUsada = vuelo.getCapacidadUsada() + productosCreados.size();
            vuelo.setCapacidadUsada(nuevaCapacidadUsada);
            vueloService.actualizar(vuelo.getId(), vuelo);
            
            log.info("✓ Vuelo {} actualizado: capacidad {}/{}",
                vuelo.getIdentificadorVuelo(),
                nuevaCapacidadUsada,
                vuelo.getCapacidadMaxima()
            );
        }
        
        // 7. Construir respuesta
        long duration = System.currentTimeMillis() - startTime;
        
        List<String> rutaAsignada = ruta.getVuelos().stream()
            .map(Vuelo::getIdentificadorVuelo)
            .collect(Collectors.toList());
        
        log.info("========================================");
        log.info("✅ ASIGNACIÓN INCREMENTAL EXITOSA");
        log.info("Tiempo de ejecución: {}ms", duration);
        log.info("========================================\n");
        
        return ResultadoAsignacionIncrementalDTO.builder()
            .exitoso(true)
            .pedidoId(pedidoId)
            .productosAsignados(productosCreados.size())
            .codigoVuelo(ruta.getVuelos().get(0).getIdentificadorVuelo())
            .rutaAsignada(rutaAsignada)
            .mensaje("Pedido asignado exitosamente sin re-optimización")
            .productosCreados(productosCreados)
            .capacidadDisponibleRestante(ruta.getInstancias().get(0).getCapacidadDisponible())
            .tiempoEjecucionMs(duration)
            .seEjecutoReoptimizacion(false)
            .build();
    }
    
    /**
     * Carga las capacidades actuales de todas las instancias de vuelo.
     * Cuenta productos asignados agrupados por instancia de vuelo.
     * 
     * @return Mapa de instancia de vuelo -> cantidad de productos asignados
     */
    private Map<String, Integer> cargarCapacidadesActuales() {
        // Cargar productos existentes agrupados por instancia de vuelo
        List<Producto> productosExistentes = productoRepository.findProductosConInstanciaAsignada();
        
        Map<String, Integer> capacidades = new HashMap<>();
        for (Producto p : productosExistentes) {
            if (p.getInstanciaVueloAsignada() != null) {
                capacidades.merge(p.getInstanciaVueloAsignada(), 1, Integer::sum);
            }
        }
        
        return capacidades;
    }
    
    /**
     * Busca una ruta con espacio disponible para el pedido.
     * Primero intenta vuelos directos, luego rutas con escalas.
     * 
     * @param pedido Pedido a asignar
     * @param vuelos Lista de vuelos disponibles
     * @param capacidadesActuales Mapa de capacidades actuales
     * @param tiempoActual Tiempo actual de simulación
     * @return Ruta encontrada, o null si no hay capacidad
     */
    private RutaEncontrada buscarRutaConEspacioDisponible(
            Pedido pedido,
            List<Vuelo> vuelos,
            Map<String, Integer> capacidadesActuales,
            LocalDateTime tiempoActual) {
        
        log.info("Buscando ruta con espacio disponible...");
        
        // Obtener aeropuertos de origen y destino
        Aeropuerto origen = aeropuertoService.buscarPorCodigoIATA(pedido.getAeropuertoOrigenCodigo()).orElse(null);
        Aeropuerto destino = aeropuertoService.buscarPorCodigoIATA(pedido.getAeropuertoDestinoCodigo()).orElse(null);
        
        if (origen == null || destino == null) {
            log.error("Aeropuerto no encontrado - origen: {}, destino: {}",
                pedido.getAeropuertoOrigenCodigo(),
                pedido.getAeropuertoDestinoCodigo()
            );
            return null;
        }
        
        int cantidadRequerida = pedido.getCantidadProductosRapido();
        
        // ESTRATEGIA 1: Buscar vuelo directo con capacidad
        log.info("Buscando vuelo directo...");
        for (Vuelo vuelo : vuelos) {
            if (vuelo.getAeropuertoOrigen().getId().equals(origen.getId()) &&
                vuelo.getAeropuertoDestino().getId().equals(destino.getId())) {
                
                log.debug("Evaluando vuelo directo: {}", vuelo.getIdentificadorVuelo());
                
                // Expandir este vuelo para obtener próximas instancias
                LocalDateTime ventanaFin = tiempoActual.plusDays(1);
                List<InstanciaVuelo> instancias = servicioExpansion.expandirVuelosParaSimulacion(
                    Collections.singletonList(vuelo),
                    tiempoActual,
                    ventanaFin
                );
                
                // Verificar capacidad en cada instancia
                for (InstanciaVuelo instancia : instancias) {
                    int usada = capacidadesActuales.getOrDefault(instancia.getIdInstancia(), 0);
                    int disponible = instancia.getCapacidadMaxima() - usada;
                    
                    log.debug("  Instancia {}: {}/{} (disponible: {})",
                        instancia.getIdInstancia(),
                        usada,
                        instancia.getCapacidadMaxima(),
                        disponible
                    );
                    
                    if (disponible >= cantidadRequerida) {
                        log.info("✓ Vuelo directo encontrado: {} con {} espacios disponibles",
                            vuelo.getIdentificadorVuelo(),
                            disponible
                        );
                        
                        instancia.setCapacidadUsada(usada);
                        return new RutaEncontrada(
                            Collections.singletonList(vuelo),
                            Collections.singletonList(instancia)
                        );
                    }
                }
            }
        }
        
        log.info("✗ No se encontró vuelo directo con capacidad");
        
        // ESTRATEGIA 2: Buscar ruta con 1 escala
        // TODO: Implementar búsqueda de rutas con escalas
        // Por ahora, solo soportamos vuelos directos para mantener simplicidad
        
        return null;
    }
    
    /**
     * Crea productos y los asigna a la ruta encontrada.
     * 
     * @param pedido Pedido del que crear productos
     * @param vuelos Lista de vuelos en la ruta
     * @param instancias Lista de instancias de vuelo correspondientes
     * @return Lista de productos creados y guardados
     */
    private List<Producto> crearYAsignarProductos(
            Pedido pedido,
            List<Vuelo> vuelos,
            List<InstanciaVuelo> instancias) {
        
        List<Producto> productos = new ArrayList<>();
        int cantidad = pedido.getCantidadProductosRapido();
        
        // Crear productos individuales
        for (int i = 0; i < cantidad; i++) {
            Producto producto = new Producto();
            producto.setPedido(pedido);
            producto.setNombre("Producto " + (i + 1) + " - Pedido " + pedido.getId());
            producto.setPeso(1.0); // Peso unitario por defecto
            producto.setVolumen(1.0); // Volumen unitario por defecto
            // Usar EN_ALMACEN como estado inicial (equivalente a PENDING)
            producto.setEstado(EstadoProducto.EN_ALMACEN);
            
            // Asignar a la primera instancia de vuelo (para rutas multi-tramo,
            // se asignaría a cada tramo pero por simplicidad usamos solo la primera)
            producto.setInstanciaVueloAsignada(instancias.get(0).getIdInstancia());
            
            // Guardar producto
            Producto guardado = productoRepository.save(producto);
            productos.add(guardado);
        }
        
        return productos;
    }
    
    /**
     * Clase interna para representar una ruta encontrada.
     */
    @Data
    @AllArgsConstructor
    private static class RutaEncontrada {
        private List<Vuelo> vuelos;
        private List<InstanciaVuelo> instancias;
    }
}

