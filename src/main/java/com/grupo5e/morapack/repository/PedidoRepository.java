package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByClienteId(Long clienteId);
    List<Pedido> findByEstado(EstadoPedido estado);
    
    /**
     * Busca pedidos dentro de una ventana de tiempo (para escenarios diarios/semanales)
     * 
     * @param fechaInicio Fecha de inicio de la ventana (inclusive)
     * @param fechaFin Fecha de fin de la ventana (inclusive)
     * @return Lista de pedidos cuya fechaPedido está dentro del rango
     */
    List<Pedido> findByFechaPedidoBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);
}
