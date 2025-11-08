package com.grupo5e.morapack.algorithm.tracking;

import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.model.Vuelo;

import java.util.*;

/**
 * ProductTracker - Mapeo directo Producto → Ruta
 * 
 * Arquitectura simplificada:
 * - Sin IDs virtuales
 * - Mapeo directo: Map<Producto, Ruta>
 * - Conteo dinámico: map.size()
 * 
 * @version 2.0 - Simplificado
 */
public class ProductTracker {
    
    private final Map<Producto, ArrayList<Vuelo>> productoToRuta;
    
    public ProductTracker() {
        this.productoToRuta = new HashMap<>();
    }
    
    /**
     * Asigna una ruta a un producto
     */
    public void assignRoute(Producto producto, ArrayList<Vuelo> ruta) {
        if (producto == null || ruta == null) {
            return;
        }
        productoToRuta.put(producto, new ArrayList<>(ruta));
    }
    
    /**
     * Obtiene la solución completa
     */
    public Map<Producto, ArrayList<Vuelo>> getSolucion() {
        return new HashMap<>(productoToRuta);
    }
    
    /**
     * Estadísticas de asignación
     */
    public Map<String, Object> getStatistics() {
        int total = productoToRuta.size();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", total);
        stats.put("assignedProducts", total);
        stats.put("unassignedProducts", 0);
        stats.put("assignmentRate", 100.0);
        
        return stats;
    }
    
    /**
     * Limpia todos los datos
     */
    public void clear() {
        productoToRuta.clear();
    }
}
