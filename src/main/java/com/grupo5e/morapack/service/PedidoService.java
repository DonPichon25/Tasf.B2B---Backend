package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.api.dto.CrearPedidoEnVivoDTO;
import java.util.List;

public interface PedidoService {
    List<Pedido> listar();
    Integer insertar(Pedido pedido);
    Pedido crearPedidoEnVivo(CrearPedidoEnVivoDTO dto);
    Pedido actualizar(Integer id, Pedido pedido);
    Pedido buscarPorId(Integer id);
    Pedido buscarPorExternalId(String externalId);
    List<Pedido> buscarPorCliente(Long clienteId);
    List<Pedido> buscarPorEstado(EstadoPedido estado);
    Pedido actualizarEstado(Integer id, EstadoPedido nuevoEstado);
    void eliminar(Integer id);
    boolean existePorId(Integer id);
    List<Pedido> insertarBulk(List<Pedido> pedidos);
    void limpiarBD();
    int contarPedidosTipoData0();
    /**
     * OPTIMIZACIÓN: Buscar múltiples pedidos por IDs en una sola query.
     * Más eficiente que llamar buscarPorId() N veces.
     */
    List<Pedido> buscarPorIds(List<Integer> ids);
}
