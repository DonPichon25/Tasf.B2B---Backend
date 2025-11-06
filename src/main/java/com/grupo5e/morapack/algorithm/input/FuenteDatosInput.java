package com.grupo5e.morapack.algorithm.input;

import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Vuelo;

import java.util.List;

/**
 * Interfaz para abstraer las fuentes de datos del algoritmo ALNS.
 * Permite cambiar entre modo ARCHIVO (data/*.txt) y BASEDATOS (PostgreSQL)
 * sin modificar la lógica del algoritmo.
 */
public interface FuenteDatosInput {
    
    /**
     * Inicializa recursos necesarios por la fuente de datos
     */
    void inicializar();
    
    /**
     * Obtiene el nombre identificador de esta fuente de datos
     * @return "ARCHIVO" o "BASEDATOS"
     */
    String obtenerNombreFuente();
    
    /**
     * Carga todos los aeropuertos desde la fuente de datos
     * @return Lista de aeropuertos con información completa
     */
    List<Aeropuerto> cargarAeropuertos();
    
    /**
     * Carga todos los vuelos desde la fuente de datos
     * @param aeropuertos Lista de aeropuertos para vincular vuelos
     * @return Lista de vuelos con información completa
     */
    List<Vuelo> cargarVuelos(List<Aeropuerto> aeropuertos);
    
    /**
     * Carga todos los pedidos desde la fuente de datos
     * @param aeropuertos Lista de aeropuertos para vincular pedidos
     * @return Lista de pedidos con información completa (incluyendo productos)
     */
    List<Pedido> cargarPedidos(List<Aeropuerto> aeropuertos);
}

