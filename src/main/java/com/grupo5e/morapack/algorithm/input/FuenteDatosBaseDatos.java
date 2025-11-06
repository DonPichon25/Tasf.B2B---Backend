package com.grupo5e.morapack.algorithm.input;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Vuelo;
import com.grupo5e.morapack.repository.AeropuertoRepository;
import com.grupo5e.morapack.repository.PedidoRepository;
import com.grupo5e.morapack.repository.VueloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    
    @Override
    public void inicializar() {
        // Spring ya inicializó los repositories
    }
    
    @Override
    public String obtenerNombreFuente() {
        return "BASEDATOS";
    }
    
    @Override
    public List<Aeropuerto> cargarAeropuertos() {
        try {
            List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
            System.out.println("✓ Cargados " + aeropuertos.size() + " aeropuertos desde BD");
            return aeropuertos;
        } catch (Exception e) {
            System.err.println("✗ Error cargando aeropuertos desde BD: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    @Override
    public List<Vuelo> cargarVuelos(List<Aeropuerto> aeropuertos) {
        try {
            List<Vuelo> vuelos = vueloRepository.findAll();
            System.out.println("✓ Cargados " + vuelos.size() + " vuelos desde BD");
            return vuelos;
        } catch (Exception e) {
            System.err.println("✗ Error cargando vuelos desde BD: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    @Override
    public List<Pedido> cargarPedidos(List<Aeropuerto> aeropuertos) {
        try {
            List<Pedido> pedidos = pedidoRepository.findAll();
            System.out.println("✓ Cargados " + pedidos.size() + " pedidos desde BD");
            
            // DEBUG: Verificar productos en cada pedido
            int totalProductos = 0;
            for (Pedido pedido : pedidos) {
                int cantProductos = (pedido.getProductos() != null) ? pedido.getProductos().size() : 0;
                totalProductos += cantProductos;
                if (cantProductos == 0) {
                    System.out.println("  ⚠️ Pedido ID " + pedido.getId() + " tiene 0 productos");
                } else {
                    System.out.println("  ✓ Pedido ID " + pedido.getId() + " tiene " + cantProductos + " productos");
                }
            }
            System.out.println("✓ Total productos en todos los pedidos: " + totalProductos);
            
            return pedidos;
        } catch (Exception e) {
            System.err.println("✗ Error cargando pedidos desde BD: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}

