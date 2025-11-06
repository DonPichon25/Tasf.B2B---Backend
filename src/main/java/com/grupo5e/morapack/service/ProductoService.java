package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.enums.EstadoProducto;

import java.util.List;

public interface ProductoService {
    List<Producto> listar();
    Integer insertar(Producto producto);
    Producto actualizar(Integer id, Producto producto);
    Producto buscarPorId(Integer id);
    List<Producto> buscarPorPedido(Integer pedidoId);
    List<Producto> buscarPorEstado(EstadoProducto estado);
    void eliminar(Integer id);
    boolean existePorId(Integer id);
    List<Producto> insertarBulk(List<Producto> productos);
}
