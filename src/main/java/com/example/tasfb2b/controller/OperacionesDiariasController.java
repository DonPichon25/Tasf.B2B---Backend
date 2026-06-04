package com.example.tasfb2b.controller;

import com.example.tasfb2b.model.Aeropuerto;
import com.example.tasfb2b.model.Pedido;
import com.example.tasfb2b.model.PedidoManualDTO;
import com.example.tasfb2b.model.Solucion;
import com.example.tasfb2b.model.Vuelo;
import com.example.tasfb2b.repository.AeropuertoRepository;
import com.example.tasfb2b.repository.PedidoRepository;
import com.example.tasfb2b.repository.VueloRepository;
import com.example.tasfb2b.service.TabuSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/diario")
@CrossOrigin(origins = "*")
public class OperacionesDiariasController {

    private final PedidoRepository pedidoRepository;
    private final AeropuertoRepository aeropuertoRepository;
    private final VueloRepository vueloRepository;
    private final TabuSearchService tabuSearchService;
    private final JdbcTemplate jdbc;

    private static final RowMapper<Pedido> PEDIDO_MAPPER = (rs, rowNum) -> {
        Pedido p = new Pedido();
        p.setIdPedido(rs.getString("id_pedido"));
        p.setOrigen(rs.getString("origen"));
        p.setDestino(rs.getString("destino"));
        p.setFechaRegistro(rs.getObject("fecha_registro", LocalDateTime.class));
        p.setCantidadMaletas(rs.getInt("cantidad_maletas"));
        p.setIdCliente(rs.getString("id_cliente"));
        return p;
    };

    public OperacionesDiariasController(PedidoRepository pedidoRepository,
                                        AeropuertoRepository aeropuertoRepository,
                                        VueloRepository vueloRepository,
                                        TabuSearchService tabuSearchService,
                                        JdbcTemplate jdbc) {
        this.pedidoRepository = pedidoRepository;
        this.aeropuertoRepository = aeropuertoRepository;
        this.vueloRepository = vueloRepository;
        this.tabuSearchService = tabuSearchService;
        this.jdbc = jdbc;
    }

    // 1. ENDPOINT PARA REGISTRAR PEDIDO MANUAL
    @PostMapping("/pedido-manual")
    public Pedido registrarPedidoManual(@RequestBody PedidoManualDTO dto) {

        // Limpiamos los strings por seguridad
        String origen = dto.getOrigen() != null ? dto.getOrigen().trim().toUpperCase() : "";
        String destino = dto.getDestino() != null ? dto.getDestino().trim().toUpperCase() : "";

        // VALIDACIÓN 1: 4 caracteres y diferente destino
        if (origen.length() != 4 || destino.length() != 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los códigos de origen y destino deben tener exactamente 4 caracteres.");
        }
        if (origen.equals(destino)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El origen y el destino no pueden ser el mismo aeropuerto.");
        }

        // VALIDACIÓN 2: Existencia real en la Base de Datos
        if (!aeropuertoRepository.existsById(origen)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El aeropuerto de origen (" + origen + ") no existe en la red.");
        }
        if (!aeropuertoRepository.existsById(destino)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El aeropuerto de destino (" + destino + ") no existe en la red.");
        }

        // Si pasa todas las validaciones, procedemos a guardar
        Pedido p = new Pedido();
        // Generamos un código único reconocible para diferenciarlo de la carga masiva
        p.setIdPedido("MANUAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setOrigen(origen);
        p.setDestino(destino);
        p.setCantidadMaletas(dto.getCantidadMaletas());
        p.setIdCliente(dto.getIdCliente());
        // Se registra exactamente con la hora que el operador ve en el reloj virtual del mapa
        p.setFechaRegistro(LocalDateTime.parse(dto.getFechaHoraVirtual()));

        return pedidoRepository.save(p);
    }

    // 2. ENDPOINT PARA OPTIMIZAR LA VENTANA ACTUAL (Rolling Window)
    @GetMapping("/ventana")
    public Solucion ejecutarVentanaDiaria(
            @RequestParam(name = "fechaInicioSimulacion") String fechaInicioSimulacionStr, // NUEVO
            @RequestParam(name = "fechaHoraActual") String fechaHoraActualStr,
            @RequestParam(name = "ventanaMinutos", defaultValue = "5") int ventanaMinutos
    ) {
        LocalDateTime inicioDeSimulacion = LocalDateTime.parse(fechaInicioSimulacionStr);
        LocalDateTime horaActualVirtual = LocalDateTime.parse(fechaHoraActualStr);
        LocalDateTime finVentanaVirtual = horaActualVirtual.plusMinutes(ventanaMinutos);
        LocalDateTime inicioHistorial = horaActualVirtual.minusDays(3);

        List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
        List<Vuelo> vuelos = vueloRepository.findAll();

        // A. Histórico del mismo día: Pedidos que ya se procesaron desde las 00:00 hasta el minuto actual.
        // Esto reconstruye fielmente la ocupación de la red para que actúe como el Warm-Up logístico.
        List<Pedido> pedidosDelPasadoSurgidosHoy = jdbc.query(
                "SELECT id_pedido, origen, destino, fecha_registro, cantidad_maletas, id_cliente " +
                        "FROM pedidos WHERE fecha_registro >= ? AND fecha_registro < ? ORDER BY fecha_registro",
                PEDIDO_MAPPER, inicioHistorial, horaActualVirtual);

        // B. Ventana Actual (Sc): Los pedidos que acaban de entrar en estos 5 minutos virtuales
        // (¡Aquí entran tanto los del archivo .txt como los manuales que registraste!)
        List<Pedido> pedidosNuevosVentana = jdbc.query(
                "SELECT id_pedido, origen, destino, fecha_registro, cantidad_maletas, id_cliente " +
                        "FROM pedidos WHERE fecha_registro >= ? AND fecha_registro < ? ORDER BY fecha_registro",
                PEDIDO_MAPPER, horaActualVirtual, finVentanaVirtual);

        // Si no hay pedidos nuevos en estos 5 minutos, retornamos una solución vacía para que el mapa no se altere
        if (pedidosNuevosVentana.isEmpty()) {
            Solucion solucionVacia = new Solucion();
            // Pasamos el histórico a su parámetro correspondiente, y la lista vacía a los pedidos a optimizar.
// Además, le pasamos 0 iteraciones porque no hay nada nuevo que optimizar con Tabú.
            Solucion simulacionPasada = tabuSearchService.ejecutarOptimizacion(
                    pedidosDelPasadoSurgidosHoy,
                    List.of(),
                    vuelos,
                    aeropuertos,
                    0
            );
            solucionVacia.setOcupacionVuelos(simulacionPasada.getOcupacionVuelos());
            solucionVacia.setOcupacionAeropuertos(simulacionPasada.getOcupacionAeropuertos());

            solucionVacia.setRutasAsignadas(simulacionPasada.getRutasAsignadas());
            solucionVacia.setCapacidadesVuelos(simulacionPasada.getCapacidadesVuelos());

            return solucionVacia;
        }
        // Ejecutamos la búsqueda Tabú tradicional: junta el pasado del día con los nuevos ingresos
        return tabuSearchService.ejecutarOptimizacion(pedidosDelPasadoSurgidosHoy, pedidosNuevosVentana, vuelos, aeropuertos, 20);
    }
}