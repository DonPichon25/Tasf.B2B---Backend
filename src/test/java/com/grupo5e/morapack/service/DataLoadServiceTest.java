package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Pedido;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataLoadServiceTest {

    @Test
    public void obtenerEstadoCuentaSoloTipoData0() {
        // Mock PedidoService
        PedidoService pedidoService = Mockito.mock(PedidoService.class);
        ClienteService clienteService = Mockito.mock(ClienteService.class);
        AeropuertoService aeropuertoService = Mockito.mock(AeropuertoService.class);
        com.grupo5e.morapack.service.impl.BatchService batchService = Mockito.mock(com.grupo5e.morapack.service.impl.BatchService.class);

        // Crear pedidos con distintos tipoData
        Pedido p1 = new Pedido(); p1.setTipoData(0);
        Pedido p2 = new Pedido(); p2.setTipoData(1);
        Pedido p3 = new Pedido(); p3.setTipoData(0);

        Mockito.when(pedidoService.listar()).thenReturn(List.of(p1,p2,p3));
        Mockito.when(aeropuertoService.listar()).thenReturn(List.of());

        DataLoadService svc = new DataLoadService(pedidoService, clienteService, aeropuertoService, batchService);

        var estado = svc.obtenerEstadoDatosNoDiario();

        assertEquals(2, estado.getTotalPedidos());
    }
}

