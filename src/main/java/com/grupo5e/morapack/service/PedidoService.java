package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.enums.EstadoPedido;

import java.util.List;

public interface PedidoService {
    List<Pedido> listar();
    Integer insertar(Pedido pedido);
    Pedido actualizar(Integer id, Pedido pedido);
    Pedido buscarPorId(Integer id);
    List<Pedido> buscarPorCliente(Long clienteId);
    List<Pedido> buscarPorEstado(EstadoPedido estado);
    Pedido actualizarEstado(Integer id, EstadoPedido nuevoEstado);
    void eliminar(Integer id);
    boolean existePorId(Integer id);
    List<Pedido> insertarBulk(List<Pedido> pedidos);
}
