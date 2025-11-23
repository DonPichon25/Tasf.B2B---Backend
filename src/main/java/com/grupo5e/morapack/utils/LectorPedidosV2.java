package com.grupo5e.morapack.utils;

import com.grupo5e.morapack.core.enums.EstadoProducto;
import com.grupo5e.morapack.core.enums.EstadoPedido;
import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.model.*;
import com.grupo5e.morapack.service.ClienteService;
import com.grupo5e.morapack.service.PedidoService;
import com.grupo5e.morapack.service.impl.BatchService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lector de Pedidos V2 - Formato del MoraPack-Backend
 * 
 * Lee archivos con patrón: _pedidos_{AIRPORT}_.txt
 * Formato de línea: id_pedido-aaaammdd-hh-mm-dest-###-IdClien
 * Ejemplo: 000000001-20250102-01-18-SPIM-003-0027081
 * 
 * Diferencias con V1:
 * - 7 campos separados por guiones (no 6 con TAB)
 * - Múltiples archivos por aeropuerto de origen
 * - Fechas completas (aaaammdd) no días de prioridad
 * - ID de pedido explícito en el archivo
 */
public class LectorPedidosV2 {
    private final String directorioDatos;
    private final List<Aeropuerto> aeropuertos;
    private final Map<String, Aeropuerto> mapaAeropuertos;

    // Servicios necesarios
    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final BatchService batchService;

    // Caché de clientes para evitar búsquedas repetidas
    private Map<UsuarioId, Cliente> cacheClientes = new HashMap<>();
    
    // Lista de clientes nuevos pendientes de guardar en batch
    private final List<Cliente> clientesNuevosPendientes = new ArrayList<>();

    public LectorPedidosV2(String directorioDatos,
                          List<Aeropuerto> aeropuertos,
                          PedidoService pedidoService,
                          ClienteService clienteService,
                          BatchService batchService) {
        this.directorioDatos = directorioDatos;
        this.aeropuertos = aeropuertos;
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.batchService = batchService;
        this.mapaAeropuertos = crearMapaAeropuertos();
    }

    private Map<String, Aeropuerto> crearMapaAeropuertos() {
        Map<String, Aeropuerto> mapa = new HashMap<>();
        for (Aeropuerto a : aeropuertos) {
            if (a.getCodigoIATA() != null) {
                mapa.put(a.getCodigoIATA().trim().toUpperCase(), a);
            }
        }
        return mapa;
    }

    /**
     * Lee y guarda pedidos desde todos los archivos _pedidos_{AIRPORT}_
     * 
     * @param horaInicioSimulacion Opcional: solo cargar pedidos después de esta hora
     * @param horaFinSimulacion Opcional: solo cargar pedidos antes de esta hora
     * @return Resultado con estadísticas de la carga
     */
    public ResultadoCargaPedidos leerYGuardarPedidos(
            LocalDateTime horaInicioSimulacion,
            LocalDateTime horaFinSimulacion) {
        
        ResultadoCargaPedidos resultado = new ResultadoCargaPedidos();
        File directorio = new File(directorioDatos);

        if (!directorio.exists() || !directorio.isDirectory()) {
            resultado.exito = false;
            resultado.mensajeError = "Directorio no encontrado: " + directorioDatos;
            System.err.println("ERROR: " + resultado.mensajeError);
            return resultado;
        }

        // Buscar todos los archivos con patrón _pedidos_{AIRPORT}_ o _pedidos_{AIRPORT}_.txt
        File[] archivosPedidos = directorio.listFiles((dir, nombre) ->
                nombre.startsWith("_pedidos_") && 
                (nombre.endsWith("_") || nombre.endsWith("_.txt") || nombre.endsWith(".txt"))
        );

        if (archivosPedidos == null || archivosPedidos.length == 0) {
            resultado.exito = false;
            resultado.mensajeError = "No se encontraron archivos con patrón _pedidos_{AIRPORT}_";
            System.err.println("WARNING: " + resultado.mensajeError);
            return resultado;
        }

        System.out.println("========================================");
        System.out.println("CARGANDO PEDIDOS DESDE ARCHIVOS V2");
        System.out.println("Directorio: " + directorioDatos);
        System.out.println("Archivos encontrados: " + archivosPedidos.length);
        if (horaInicioSimulacion != null && horaFinSimulacion != null) {
            System.out.println("Ventana de tiempo: " + horaInicioSimulacion + " a " + horaFinSimulacion);
        } else {
            System.out.println("Ventana de tiempo: TODOS LOS PEDIDOS (sin filtrado)");
        }
        System.out.println("========================================");

        LocalDateTime tiempoInicio = LocalDateTime.now();


        // Procesar cada archivo
        for (File archivo : archivosPedidos) {
            String nombreArchivo = archivo.getName();
            this.cacheClientes = obtenerTodosClientesMapeados(0);
            // Extraer código de aeropuerto del nombre
            // Ejemplo: _pedidos_SPIM_ -> SPIM o _pedidos_EBCI_.txt -> EBCI
            String codigoAeropuertoDestino = nombreArchivo
                    .replace("_pedidos_", "")
                    .replace(".txt", "")
                    .replace("_", "")
                    .trim()
                    .toUpperCase();
//
//            Aeropuerto aeropuertoOrigen = mapaAeropuertos.get(colocarAeropuertoPrincipalAleatorio(codigoAeropuertoDestino));
//            if (aeropuertoOrigen == null) {
//                System.err.println("WARNING: Aeropuerto origen desconocido: " + codigoAeropuertoDestino + " en " + nombreArchivo);
//                resultado.erroresArchivos++;
//                continue;
//            }

            System.out.println("\nProcesando archivo: " + nombreArchivo + " (destino: " + codigoAeropuertoDestino + ")");

            try {
                procesarArchivoPedidos(archivo, horaInicioSimulacion, horaFinSimulacion, resultado);
            } catch (Exception e) {
                System.err.println("ERROR procesando archivo " + nombreArchivo + ": " + e.getMessage());
                e.printStackTrace();
                resultado.erroresArchivos++;
            }
        }

        resultado.tiempoFin = LocalDateTime.now();
        resultado.duracionSegundos = ChronoUnit.SECONDS.between(tiempoInicio, resultado.tiempoFin);
        resultado.exito = resultado.erroresArchivos == 0 && resultado.pedidosCargados > 0;

        System.out.println("\n========================================");
        System.out.println("RESUMEN DE CARGA DE PEDIDOS");
        System.out.println("Total de pedidos cargados: " + resultado.pedidosCargados);
        System.out.println("Total de pedidos creados: " + resultado.pedidosCreados);
        System.out.println("Pedidos filtrados (fuera de ventana): " + resultado.pedidosFiltrados);
        System.out.println("Errores de parseo: " + resultado.erroresParseo);
        System.out.println("Errores de archivos: " + resultado.erroresArchivos);
        System.out.println("Duración: " + resultado.duracionSegundos + " segundos");
        System.out.println("========================================");

        return resultado;
    }
    public Map<UsuarioId, Cliente> obtenerTodosClientesMapeados(int tipoData) {
        List<Cliente> lista = clienteService.listarPorTipoData(tipoData);
        return lista.stream().collect(Collectors.toMap(
                Cliente::getUsuarioId,
                c -> c
        ));
    }

    private void procesarArchivoPedidos(
            File archivo,
            LocalDateTime horaInicio,
            LocalDateTime horaFin,
            ResultadoCargaPedidos resultado) throws IOException {
        int i=1;
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int numeroLinea = 0;
            List<Pedido> pedidosPorCrear = new ArrayList<>();

            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                linea = linea.trim();

                if (linea.isEmpty()) {
                    continue;
                }

                try {
                    Pedido pedido = parsearLineaPedido(linea);
                    resultado.pedidosCargados++;
                    //System.out.println("  ➕ Pedido parseado: " + i);
                    i++;
                    // Filtrar por ventana de tiempo si se especificó
                    if (horaInicio != null && horaFin != null) {
                        if (pedido.getFechaPedido().isBefore(horaInicio) ||
                                pedido.getFechaPedido().isAfter(horaFin)) {
                            resultado.pedidosFiltrados++;
                            continue;
                        }
                    }

                    pedidosPorCrear.add(pedido);

                    // OPTIMIZACIÓN: Guardar clientes cada 1000 pedidos para evitar problemas de flush
                    if (clientesNuevosPendientes.size() >= 1000) {
                        guardarClientesPendientes();
                    }

                    // 🚀 OPTIMIZADO: Lotes de 500 para mejor aprovechamiento del batch
                    if (pedidosPorCrear.size() >= 500) {
                        // CRÍTICO: Guardar clientes pendientes ANTES de guardar pedidos
                        // para evitar errores
                        if (!clientesNuevosPendientes.isEmpty()) {
                            guardarClientesPendientes();
                        }
                        guardarLotePedidos(pedidosPorCrear, resultado);
                        pedidosPorCrear.clear();
                    }

                } catch (Exception e) {
                    resultado.erroresParseo++;
                    System.err.println("Error parseando línea " + numeroLinea + ": " + e.getMessage());
                }
            }

            // CRÍTICO: Guardar clientes ANTES de los pedidos restantes
            if (!clientesNuevosPendientes.isEmpty()) {
                guardarClientesPendientes();
            }
            
            // Guardar pedidos restantes
            if (!pedidosPorCrear.isEmpty()) {
                guardarLotePedidos(pedidosPorCrear, resultado);
            }

            System.out.println("  Líneas procesadas: " + numeroLinea);
        }
    }

    /**
     * Parsea una línea del archivo en formato V2
     * Formato: id_pedido-aaaammdd-hh-mm-dest-###-IdClien
     * Ejemplo: 000000001-20250102-01-18-SPIM-003-0027081
     */
    private Pedido parsearLineaPedido(String linea) {
        String[] partes = linea.split("-");
        if (partes.length != 7) {
            throw new IllegalArgumentException("Formato inválido: esperado 7 campos, encontrado " + partes.length);
        }

        // Parsear campos
        String idPedidoStr = partes[0];
        String fechaStr = partes[1];  // aaaammdd
        int hora = Integer.parseInt(partes[2]);
        int minuto = Integer.parseInt(partes[3]);
        String codigoAeropuertoDestino = partes[4].trim().toUpperCase();
        int cantidadProductos = Integer.parseInt(partes[5]);
        String idClienteStr = partes[6];

        // Parsear fecha (aaaammdd -> LocalDateTime)
        int anio = Integer.parseInt(fechaStr.substring(0, 4));
        int mes = Integer.parseInt(fechaStr.substring(4, 6));
        int dia = Integer.parseInt(fechaStr.substring(6, 8));
        LocalDateTime fechaPedido = LocalDateTime.of(anio, mes, dia, hora, minuto, 0);

        // Buscar aeropuerto origen (asumido como aeropuerto principal aleatorio)
        Aeropuerto aeropuertoOrigen = mapaAeropuertos.get(colocarAeropuertoPrincipalAleatorio(codigoAeropuertoDestino));
        if (aeropuertoOrigen == null) {
            throw new IllegalArgumentException("Aeropuerto origen desconocido para destino: " + codigoAeropuertoDestino);
        }
        // Buscar aeropuerto destino
        Aeropuerto aeropuertoDestino = mapaAeropuertos.get(codigoAeropuertoDestino);
        if (aeropuertoDestino == null) {
            throw new IllegalArgumentException("Aeropuerto destino desconocido: " + codigoAeropuertoDestino);
        }

        // Calcular plazo de entrega (regla simple: +3 días)
        // TODO: Implementar lógica basada en continentes como en el Backend
        LocalDateTime fechaLimiteEntrega = fechaPedido.plusDays(3);

        // Obtener o crear cliente
        Cliente cliente = obtenerOCrearCliente(idClienteStr, aeropuertoDestino.getCiudad());

        // Crear pedido
        Pedido pedido = new Pedido();
        
        // Generar externalId compuesto: {AIRPORT_ORIGIN}-{FILE_ORDER_ID}
        String externalId = aeropuertoDestino.getCodigoIATA() + "-" + idPedidoStr;
        pedido.setExternalId(externalId);
        
        pedido.setNombre("PEDIDO-" + idPedidoStr + "-" + codigoAeropuertoDestino);
        pedido.setCliente(cliente);
        pedido.setAeropuertoOrigenCodigo(aeropuertoOrigen.getCodigoIATA());
        pedido.setAeropuertoDestinoCodigo(aeropuertoDestino.getCodigoIATA());
        pedido.setFechaPedido(fechaPedido);
        pedido.setFechaLimiteEntrega(fechaLimiteEntrega);
        pedido.setEstado(EstadoPedido.PENDIENTE);

        // Calcular prioridad
        double prioridad = calcularPrioridad(fechaPedido, fechaLimiteEntrega);
        pedido.setPrioridad(prioridad);

        // ⚠️ CRÍTICO: Sincronizar cantidadProductos ANTES de crear productos
        pedido.setCantidadProductos(cantidadProductos);

        // Crear productos
        ArrayList<Producto> productos = crearProductos(cantidadProductos, pedido);
        pedido.setProductos(productos);

        pedido.setTipoData(0);
        return pedido;
    }

    /**
     * Guarda los clientes pendientes en batch y actualiza el cache
     */
    private void guardarClientesPendientes() {
        if (clientesNuevosPendientes.isEmpty()) {
            return;
        }
        
        System.out.println("  💾 Guardando " + clientesNuevosPendientes.size() + " clientes nuevos en batch...");
        try {
            List<Cliente> clientesGuardados = clienteService.insertarBulk(clientesNuevosPendientes);
            
            // CRÍTICO: Actualizar caché con las instancias PERSISTIDAS retornadas por JPA
            // Esto es importante porque las instancias persistidas son "managed" por JPA
            // y tienen el estado correcto de la BD
            for (Cliente clientePersistido : clientesGuardados) {
                // Reemplazar en caché la instancia transient con la instancia persistida
                cacheClientes.put(clientePersistido.getUsuarioId(), clientePersistido);
            }
            
            System.out.println("  ✅ " + clientesGuardados.size() + " clientes guardados y caché actualizado");
        } catch (Exception e) {
            System.err.println("  ❌ Error guardando clientes en batch: " + e.getMessage());
            e.printStackTrace();
            // En caso de error, también limpiar la lista para evitar reintentos con datos inconsistentes
        }
        clientesNuevosPendientes.clear();
    }
    
    /**
     * Obtiene o crea un cliente con caché para evitar búsquedas repetidas
     * OPTIMIZADO: Acumula clientes nuevos para guardarlos en batch antes de los pedidos
     */
    private Cliente obtenerOCrearCliente(String idClienteStr, Ciudad ciudadRecojo) {

        // Key compuesto
        UsuarioId key = new UsuarioId(Long.parseLong(idClienteStr), 0);

        // Verificar caché primero
        if (cacheClientes.containsKey(key)) {
            return cacheClientes.get(key);
        }

//        // Intentar buscar en BD
//        Long idCliente = Long.parseLong(idClienteStr); // Ajuste de ID según convención
//        try {
//            Cliente clienteExistente = clienteService.buscarPorId(idCliente,0);
//            if (clienteExistente != null) {
//                cacheClientes.put(new UsuarioId(idCliente,0), clienteExistente);
//                return clienteExistente;
//            }
//        } catch (Exception e) {
//            // Cliente no existe, continuar para crearlo
//        }
        Long idCliente = Long.parseLong(idClienteStr);
        // Crear nuevo cliente (sin persistir aún)
        Cliente nuevoCliente = new Cliente();
        UsuarioId usuarioId = new UsuarioId();
        usuarioId.setId(idCliente);
        usuarioId.setTipoData(0);
        nuevoCliente.setUsuarioId(usuarioId);
        nuevoCliente.setNombres("Cliente " + idCliente);
        nuevoCliente.setCorreo("cliente" + idCliente + "@morapack.com");
        nuevoCliente.setCiudadRecojo(ciudadRecojo);
        nuevoCliente.setRol(Rol.CLIENTE);
        nuevoCliente.setUsernameOrEmail("cliente" + idCliente);
        nuevoCliente.setPassword("temporal");
        nuevoCliente.setActivo(true);

        // OPTIMIZACIÓN: Agregar a lista de pendientes en lugar de insertar inmediatamente
        clientesNuevosPendientes.add(nuevoCliente);
        
        // Guardar en caché (aunque aún no tenga ID de BD, está ok para referencias)
        cacheClientes.put(usuarioId, nuevoCliente);
        
        return nuevoCliente;
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
            producto.setTipoData(0);
            productos.add(producto);
        }
        return productos;
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

    private void guardarLotePedidos(List<Pedido> pedidos, ResultadoCargaPedidos resultado) {
        if (pedidos == null || pedidos.isEmpty()) {
            return;
        }
        
        try {
            // CRÍTICO: Asegurar que todos los pedidos tengan referencias a clientes PERSISTIDOS
            // En caso de que algún pedido tenga referencia a una instancia transient del caché antiguo,
            // actualizar con la instancia persistida del caché actualizado
            for (Pedido pedido : pedidos) {
                if (pedido.getCliente() != null && pedido.getCliente().getUsuarioId() != null) {
                    UsuarioId usuarioId = new UsuarioId();
                    usuarioId = pedido.getCliente().getUsuarioId();
                    //String clienteId = String.valueOf(pedido.getCliente().getId());
                    // Obtener la instancia persistida del caché (si existe)
                    Cliente clientePersistido = cacheClientes.get(usuarioId);
                    if (clientePersistido != null) {
                        pedido.setCliente(clientePersistido);
                    }
                }
                
                // ✅ OPTIMIZACIÓN: Sincronizar cantidadProductos antes de guardar
                // Esto asegura que el campo esté actualizado en BD
                pedido.sincronizarCantidadProductos();
            }
            
            //OPTIMIZACIÓN: Usar JDBC batch real con EntityManager , revisar bien q ue sea util para este caso:c
            // Esto reduce de N queries individuales a verdaderos batch statements
            int insertados = batchService.insertarPedidosEnBatch(pedidos);
            resultado.pedidosCreados += insertados;
            System.out.println("  ✅ Lote de " + insertados + " pedidos guardados en batch real (JDBC)");
        } catch (Exception e) {
            System.err.println("  ❌ Error guardando lote de pedidos: " + e.getMessage());
            e.printStackTrace();
            // Intentar guardar uno por uno como fallback
            System.out.println("  ⚠️ Intentando guardar pedidos individualmente como fallback...");
            int guardadosIndividualmente = 0;
            for (Pedido pedido : pedidos) {
                try {
                    pedidoService.insertar(pedido);
                    guardadosIndividualmente++;
                } catch (Exception ex) {
                    System.err.println("    Error guardando pedido individual: " + ex.getMessage());
                }
            }
            resultado.pedidosCreados += guardadosIndividualmente;
            System.out.println("  ✅ " + guardadosIndividualmente + " pedidos guardados individualmente");
        }
    }

    /**
     * Clase para almacenar resultados de la carga
     */
    public static class ResultadoCargaPedidos {
        public boolean exito;
        public String mensajeError;
        public LocalDateTime tiempoInicio;
        public LocalDateTime tiempoFin;
        public long duracionSegundos;
        public int pedidosCargados;
        public int pedidosCreados;
        public int pedidosFiltrados;
        public int erroresParseo;
        public int erroresArchivos;
    }
    // Método auxiliar para encontrar aeropuerto por defecto
    private String colocarAeropuertoPrincipalAleatorio(String codigoDestino) {
        // Lista de códigos IATA de los aeropuertos principales de MoraPack
        String[] aeropuertosPrincipales = {"SPIM", "UBBB", "EBCI"};
        ArrayList<String> aeropuertos = new ArrayList<>();

        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            if(Objects.equals(codigoDestino, aeropuertosPrincipales[i]))
                continue;
            aeropuertos.add(aeropuertosPrincipales[i]);
        }
        int indiceAleatorio = random.nextInt(aeropuertos.size());
        String codigoIATAOrigen = aeropuertos.get(indiceAleatorio);
        //System.out.println("🔀 Usando aeropuerto por defecto: " + codigoIATAOrigen);
        return codigoIATAOrigen;
    }
}

