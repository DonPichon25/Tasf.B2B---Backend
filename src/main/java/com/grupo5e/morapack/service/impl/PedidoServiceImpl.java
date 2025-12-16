package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.dto.CrearPedidoEnVivoDTO;
import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.repository.PedidoRepository;
import com.grupo5e.morapack.repository.ProductoRepository;
import com.grupo5e.morapack.service.AeropuertoService;
import com.grupo5e.morapack.service.ClienteService;
import com.grupo5e.morapack.service.PedidoService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private EntityManager entityManager;
    private final AeropuertoService aeropuertoService;
    private final ClienteService clienteService;

    public PedidoServiceImpl(PedidoRepository pedidoRepository, ProductoRepository productoRepository, AeropuertoService aeropuertoService, ClienteService clienteService) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.aeropuertoService = aeropuertoService;
        this.clienteService = clienteService;
    }

    @Override
    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    @Override
    @Transactional
    public Integer insertar(Pedido pedido) {
        return pedidoRepository.save(pedido).getId();
    }

    @Override
    @Transactional
    public Pedido actualizar(Integer id, Pedido pedido) {
        Pedido existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Pedido", "id", id);
        }
        pedido.setId(id);
        return pedidoRepository.save(pedido);
    }
    @Override
    @Transactional
    public Pedido crearPedidoEnVivo(CrearPedidoEnVivoDTO dto) {

        // 1) Calcular ID nuevo
        Integer maxId = pedidoRepository.findMaxIdByTipoData(1);
        int nuevoId = (maxId != null ? maxId : 0) + 1;

        // 2) Fecha pedido = la que viene del front (inicio próxima ventana)
        LocalDateTime fechaPedido = dto.getFechaPedido();
        if (fechaPedido == null) {
            // fallback por si acaso
            fechaPedido = LocalDateTime.now();
        }

        // 3) Fecha límite = fechaPedido + 3 días
        LocalDateTime fechaLimiteEntrega = fechaPedido.plusDays(3);

        // 4) Aeropuerto destino
        String codigoDestino = dto.getAeropuertoDestinoCodigo().trim().toUpperCase();

        Optional<Aeropuerto> optDestino = aeropuertoService.buscarPorCodigoIATA(codigoDestino);
        Aeropuerto aeropuertoDestino = optDestino.orElseThrow(
                () -> new IllegalArgumentException("Aeropuerto destino desconocido: " + codigoDestino)
        );

        // 5) Aeropuerto ORIGEN
        Optional<Aeropuerto> optOrigen = aeropuertoService.buscarPorCodigoIATA(obtenerAeropuertoOrigenParaDestino(aeropuertoDestino));
        Aeropuerto aeropuertoOrigen= optOrigen.orElseThrow(
                () -> new IllegalArgumentException("Aeropuerto destino desconocido: " + obtenerAeropuertoOrigenParaDestino(aeropuertoDestino))
        );
        // 6) Cliente (puede ser un cliente "dummy" o buscado por ciudad, como en parsearLineaPedido)
        //Cliente cliente = clienteService.buscarPorId(14064L,1);//CUIDAOOOOOOOOOOOAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
        if (dto.getClienteId() == null) {
            throw new IllegalArgumentException("clienteId es requerido para registrar el pedido.");
        }
        Cliente cliente = obtenerOCrearCliente(dto.getClienteId(), 1, aeropuertoDestino.getCiudad());
        // 7) Construir Pedido
        Pedido pedido = new Pedido();
        //pedido.setId(94872);//cambiar pa q use el nuevo id
        pedido.setTipoData(1);

        // external_id: DESTINO + "-" + id con ceros a la izquierda
        String externalId = dto.getAeropuertoDestinoCodigo() + "-" +
                String.format("%09d", nuevoId); // 9 dígitos: 000023653
        pedido.setExternalId(externalId);

        pedido.setNombre("PEDIDO-" + 94873 + "-" + codigoDestino);
        pedido.setCliente(cliente);
        //pedido.getCliente().setTipoData(1);

        pedido.setAeropuertoOrigenCodigo(aeropuertoOrigen.getCodigoIATA());
        pedido.setAeropuertoDestinoCodigo(aeropuertoDestino.getCodigoIATA());

        pedido.setFechaPedido(fechaPedido);
        pedido.setFechaLimiteEntrega(fechaLimiteEntrega);
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        // 8) Prioridad
        double prioridad = calcularPrioridad(fechaPedido, fechaLimiteEntrega);
        pedido.setPrioridad(prioridad);

        // 9) Cantidad y productos
        int cantidadProductos = dto.getCantidadProductos();
        pedido.setCantidadProductos(cantidadProductos);

        List<Producto> productos = crearProductos(cantidadProductos, pedido);
        pedido.setProductos(productos);

        // 10) Guardar
        pedidoRepository.save(pedido);
        productoRepository.saveAll(productos);

        return pedido;
    }
    private Cliente obtenerOCrearCliente(Long clienteId, Integer tipoData, Ciudad ciudadRecojo) {

        // 1) Buscar
        try {
            Cliente existente = clienteService.buscarPorId(clienteId, tipoData);
            if (existente != null) return existente;
        } catch (Exception ignored) {}

        // 2) Crear
        Cliente nuevo = new Cliente();

        UsuarioId usuarioId = new UsuarioId();
        usuarioId.setId(clienteId);
        usuarioId.setTipoData(tipoData);

        nuevo.setUsuarioId(usuarioId);
        nuevo.setNombres("Cliente " + clienteId);
        nuevo.setCorreo("cliente" + clienteId + "@morapack.com");
        nuevo.setCiudadRecojo(ciudadRecojo);

        nuevo.setRol(Rol.CLIENTE);
        nuevo.setUsernameOrEmail("cliente" + clienteId + "-" + tipoData);
        nuevo.setPassword("temporal");
        nuevo.setActivo(true);

        // Insertar y re-leer (para asegurar entidad PERSISTIDA/managed)
        try {
            clienteService.insertar(nuevo);
        } catch (Exception ignored) {
            // si falló por duplicado en carrera, igual re-leemos abajo
        }

        Cliente persistido = clienteService.buscarPorId(clienteId, tipoData);
        if (persistido == null) {
            throw new IllegalStateException("No se pudo crear/obtener cliente id=" + clienteId + " tipoData=" + tipoData);
        }
        return persistido;
    }


    // Puedes reutilizar estas helper o copiarlas de donde ya las tienes:
    private String obtenerAeropuertoOrigenParaDestino(Aeropuerto destino) {
//        // Lista de códigos IATA de los aeropuertos principales de MoraPack
//        String[] aeropuertosPrincipales = {"SPIM", "UBBB", "EBCI"};
//        ArrayList<String> aeropuertos = new ArrayList<>();
//
//        Random random = new Random();
//        for (int i = 0; i < 3; i++) {
//            if(Objects.equals(destino, aeropuertosPrincipales[i]))
//                continue;
//            aeropuertos.add(aeropuertosPrincipales[i]);
//        }
//        int indiceAleatorio = random.nextInt(aeropuertos.size());
//        String codigoIATAOrigen = aeropuertos.get(indiceAleatorio);
//        //System.out.println("🔀 Usando aeropuerto por defecto: " + codigoIATAOrigen);
//        return codigoIATAOrigen;
        String[] principales = {"SPIM", "UBBB", "EBCI"};
        ArrayList<String> candidatos = new ArrayList<>();

        String codigoDestino = destino.getCodigoIATA();

        for (String p : principales) {
            if (!Objects.equals(codigoDestino, p)) {
                candidatos.add(p);
            }
        }
        return candidatos.get(new Random().nextInt(candidatos.size()));
    }

    private double calcularPrioridad(LocalDateTime fechaPedido, LocalDateTime plazoEntrega) {
        long horas = ChronoUnit.HOURS.between(fechaPedido, plazoEntrega);

        if (horas <= 24) {
            return 1.0;
        } else if (horas <= 96) {
            return 0.75;
        } else if (horas <= 288) {
            return 0.5;
        } else {
            return 0.25;
        }
    }

    private ArrayList<Producto> crearProductos(int cantidad, Pedido pedido) {
        ArrayList<Producto> productos = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            Producto producto = new Producto();
            producto.setNombre("PRODUCT-" + (i + 1));
            producto.setPeso(1.0);  // Peso por defecto
            producto.setVolumen(1.0);  // Volumen por defecto
            producto.setEstado(EstadoProducto.EN_ALMACEN);
            producto.setPedido(pedido);
            producto.setTipoData(1);
            productos.add(producto);
        }
        return productos;
    }
    @Override
    public Pedido buscarPorId(Integer id) {
        return pedidoRepository.findById(id).orElse(null);
    }

    @Override
    public List<Pedido> buscarPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }

    @Override
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstado(estado);
    }

    @Override
    @Transactional
    public Pedido actualizarEstado(Integer id, EstadoPedido nuevoEstado) {
        Pedido pedido = buscarPorId(id);
        if (pedido == null) {
            throw new ResourceNotFoundException("Pedido", "id", id);
        }
        pedido.setEstado(nuevoEstado);
        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Pedido", "id", id);
        }
        pedidoRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Integer id) {
        return pedidoRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Pedido> insertarBulk(List<Pedido> pedidos) {
        return pedidoRepository.saveAll(pedidos).stream().collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void limpiarBD() {
        pedidoRepository.eliminarTipoDataCero();
    }
    @Override
    public Pedido buscarPorExternalId(String externalId) {
        return pedidoRepository.findByExternalId(externalId).orElse(null);
    }
    @Override
    public int contarPedidosTipoData0() {
        return pedidoRepository.contarTipoDataCero();
    }

    /**
     * OPTIMIZACIÓN: Buscar múltiples pedidos por IDs en una sola query.
     * Usa findAllById() de JpaRepository que genera un query eficiente con IN
     * clause.
     */
    @Override
    public List<Pedido> buscarPorIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return pedidoRepository.findAllById(ids);
    }
}
