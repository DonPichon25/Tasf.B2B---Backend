package com.grupo5e.morapack.algorithm.input;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Cancelacion;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.repository.AeropuertoRepository;
import com.grupo5e.morapack.repository.CancelacionRepository;
import com.grupo5e.morapack.repository.PedidoRepository;
import com.grupo5e.morapack.repository.VueloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementación de FuenteDatosInput que lee desde la base de datos PostgreSQL
 * usando repositorios JPA de Spring.
 * 
 * Permite al algoritmo trabajar con datos persistidos en BD en lugar de archivos.
 */
@Component
public class FuenteDatosBaseDatos implements FuenteDatosInput {
    
    @Autowired
    private AeropuertoRepository aeropuertoRepository;
    
    @Autowired
    private VueloRepository vueloRepository;
    
    @Autowired
    private PedidoRepository pedidoRepository;
    @Autowired
    private CancelacionRepository cancelacionRepository;
    @Override
    public void inicializar() {
        // Spring ya inicializó los repositories
    }
    
    @Override
    public String obtenerNombreFuente() {
        return "BASEDATOS";
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Aeropuerto> cargarAeropuertos() {
        try {
            List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
            
            // ✅ Forzar inicialización de relaciones LAZY para evitar LazyInitializationException
            // La anotación @Transactional mantiene la sesión de Hibernate abierta
            for (Aeropuerto aeropuerto : aeropuertos) {
                if (aeropuerto.getCiudad() != null) {
                    // Acceder al nombre fuerza la carga del proxy
                    aeropuerto.getCiudad().getNombre();
                }
                if (aeropuerto.getAlmacen() != null) {
                    // Forzar carga del almacén
                    aeropuerto.getAlmacen().getCapacidadMaxima();
                }
            }
            
            System.out.println("✓ Cargados " + aeropuertos.size() + " aeropuertos desde BD (con relaciones inicializadas)");
            return aeropuertos;
        } catch (Exception e) {
            System.err.println("✗ Error cargando aeropuertos desde BD: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Vuelo> cargarVuelos(List<Aeropuerto> aeropuertos) {
        try {
            List<Vuelo> vuelos = vueloRepository.findAll();
            
            // ✅ Forzar inicialización de relaciones LAZY
            // La anotación @Transactional mantiene la sesión de Hibernate abierta
            for (Vuelo vuelo : vuelos) {
                if (vuelo.getAeropuertoOrigen() != null) {
                    vuelo.getAeropuertoOrigen().getCodigoIATA();
                    if (vuelo.getAeropuertoOrigen().getCiudad() != null) {
                        vuelo.getAeropuertoOrigen().getCiudad().getNombre();
                    }
                }
                if (vuelo.getAeropuertoDestino() != null) {
                    vuelo.getAeropuertoDestino().getCodigoIATA();
                    if (vuelo.getAeropuertoDestino().getCiudad() != null) {
                        vuelo.getAeropuertoDestino().getCiudad().getNombre();
                    }
                }
            }
            
            System.out.println("✓ Cargados " + vuelos.size() + " vuelos desde BD (con relaciones inicializadas)");
            return vuelos;
        } catch (Exception e) {
            System.err.println("✗ Error cargando vuelos desde BD: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    @Override
    @Transactional(readOnly = true)
    public List<Cancelacion> cargarCancelaciones(List<Vuelo> vuelos) {
        try {
            List<Cancelacion> cancelaciones = cancelacionRepository.findAll();

            // ✅ Forzar inicialización de relaciones LAZY
            int conVuelo = 0;
            for (Cancelacion c : cancelaciones) {
                if (c.getVuelo() != null) {
                    conVuelo++;
                    // Forzar carga de vuelo y sus aeropuertos
                    Vuelo v = c.getVuelo();
                    v.getId(); // ID del vuelo
                    if (v.getAeropuertoOrigen() != null) {
                        v.getAeropuertoOrigen().getCodigoIATA();
                    }
                    if (v.getAeropuertoDestino() != null) {
                        v.getAeropuertoDestino().getCodigoIATA();
                    }
                }
            }

            System.out.println("✓ Cargadas " + cancelaciones.size() + " cancelaciones desde BD");
            System.out.println("✓ Cancelaciones con vuelo asociado: " + conVuelo);

            // Log chiquito de ejemplo
            cancelaciones.stream().limit(5).forEach(c -> {
                String vueloInfo = (c.getVuelo() != null)
                        ? "VueloID=" + c.getVuelo().getId()
                        : "Vuelo=NULL";
                System.out.println("   - Cancelación: dia=" + c.getDiasCancelado() +
                        " " + c.getCodigoIATAOrigen() + "→" + c.getCodigoIATADestino() +
                        " " + String.format("%02d:%02d", c.getHora(), c.getMinuto()) +
                        " | " + vueloInfo);
            });

            return cancelaciones;
        } catch (Exception e) {
            System.err.println("✗ Error cargando cancelaciones desde BD: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> cargarPedidos(List<Aeropuerto> aeropuertos) {
        try {
            // ⚠️ ADVERTENCIA: Cargar TODOS los pedidos puede ser lento con datasets grandes
            // Considera usar cargarPedidosPorVentanaDeTiempo() en su lugar
            
            List<Pedido> pedidos = pedidoRepository.findAll();
            
            // ✅ Forzar inicialización de relaciones LAZY NECESARIAS
            // La anotación @Transactional mantiene la sesión de Hibernate abierta
            int totalProductos = 0;
            for (Pedido pedido : pedidos) {
                // Forzar carga de cliente (necesario)
                if (pedido.getCliente() != null) {
                    pedido.getCliente().getNombres();
                    if (pedido.getCliente().getCiudadRecojo() != null) {
                        pedido.getCliente().getCiudadRecojo().getNombre();
                    }
                }
                
                // ⚡ OPTIMIZACIÓN: NO cargar productos aquí
                // Usar cantidadProductos directo (campo en tabla pedidos)
                int cantProductos = pedido.getCantidadProductosRapido();
                totalProductos += cantProductos;
            }
            
            System.out.println("✓ Cargados " + pedidos.size() + " pedidos desde BD (con relaciones inicializadas)");
            System.out.println("✓ Total productos en todos los pedidos: " + totalProductos + " (sin cargar lista)");
            
            return pedidos;
        } catch (Exception e) {
            System.err.println("✗ Error cargando pedidos desde BD: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    /**
     * 🚀 OPTIMIZADO: Implementación optimizada para cargar pedidos por ventana de tiempo
     * Usa query custom en lugar de cargar todo y filtrar en memoria
     */
    @Override
    @Transactional(readOnly = true)
    public List<Pedido> cargarPedidosPorVentanaDeTiempo(
            List<Aeropuerto> aeropuertos,
            LocalDateTime horaInicio,
            LocalDateTime horaFin) {
        try {
            System.out.println("========================================");
            System.out.println("CARGANDO PEDIDOS CON VENTANA DE TIEMPO (OPTIMIZADO)");
            System.out.println("Hora inicio: " + horaInicio);
            System.out.println("Hora fin: " + horaFin);
            System.out.println("========================================");
            
            long startTime = System.currentTimeMillis();
            
            // ⚡ OPTIMIZACIÓN 1: Query optimizada - solo pedidos en ventana
            List<Pedido> pedidos = pedidoRepository.findByFechaPedidoBetween(horaInicio, horaFin);
            
            long queryTime = System.currentTimeMillis();
            System.out.println("⏱️  Query ejecutada en " + (queryTime - startTime) + "ms");
            
            // ⚡ OPTIMIZACIÓN 2: Contar productos SIN cargarlos
            // Usar el campo cantidadProductos directo
            Long totalProductosQuery = pedidoRepository.sumarCantidadProductosEnRango(horaInicio, horaFin);
            int totalProductos = (totalProductosQuery != null) ? totalProductosQuery.intValue() : 0;
            
            long countTime = System.currentTimeMillis();
            System.out.println("⏱️  Conteo de productos en " + (countTime - queryTime) + "ms");
            
            // ✅ Forzar inicialización solo de relaciones NECESARIAS (cliente, ciudad)
            for (Pedido pedido : pedidos) {
                // Forzar carga de cliente (necesario para el algoritmo)
                if (pedido.getCliente() != null) {
                    pedido.getCliente().getNombres();
                    if (pedido.getCliente().getCiudadRecojo() != null) {
                        pedido.getCliente().getCiudadRecojo().getNombre();
                    }
                }
                // ⚡ NO cargar productos - se cargarán LAZY solo si se necesitan
            }
            
            long initTime = System.currentTimeMillis();
            System.out.println("⏱️  Inicialización de relaciones en " + (initTime - countTime) + "ms");
            
            System.out.println("========================================");
            System.out.println("✅ CARGA COMPLETADA");
            System.out.println("✓ Pedidos cargados: " + pedidos.size());
            System.out.println("✓ Total productos: " + totalProductos + " (conteo optimizado)");
            System.out.println("✓ Tiempo total: " + (initTime - startTime) + "ms");
            System.out.println("========================================");
            
            return pedidos;
        } catch (Exception e) {
            System.err.println("✗ Error cargando pedidos por ventana de tiempo: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}

