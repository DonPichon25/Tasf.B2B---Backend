package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.core.model.Pedido;
import com.grupo5e.morapack.core.model.Producto;
import com.grupo5e.morapack.core.model.Vuelo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 🚀 SERVICIO OPTIMIZADO PARA BATCH INSERTS MASIVOS
 * 
 * Este servicio implementa VERDADERO batch processing para TODAS las entidades:
 * - Pedidos + Productos (en cascade)
 * - Vuelos
 * - Productos individuales
 * 
 * Usa:
 * - JDBC Batch nativo vía EntityManager
 * - Flush/Clear estratégico para evitar OutOfMemory
 * - Optimizaciones de Hibernate para INSERT masivo
 * 
 * PERFORMANCE:
 * - ANTES: 50,000 queries individuales para 10K pedidos con 5 productos
 * - DESPUÉS: ~60 batch queries (1000 registros por batch)
 * 
 * @author Senior Developer
 */
@Slf4j
@Service
public class BatchService {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:1000}")
    private int batchSize;

    /**
     * 🔥 OPTIMIZADO: Inserta pedidos en verdadero batch con JDBC
     * 
     * Estrategia CONSERVADORA para evitar "delayed insert actions":
     * 1. Verifica relaciones bidireccionales pedido ↔ productos
     * 2. Usa persist() directo (más rápido que merge())
     * 3. Flush periódico SIN clear() durante el proceso
     * 4. Clear SOLO al final para evitar desconectar entidades prematuramente
     * 5. Productos se insertan automáticamente en cascade
     *
     * NOTA: Este enfoque usa más memoria pero es 100% seguro para cascade
     *
     * @param pedidos Lista de pedidos con productos
     * @return Cantidad de pedidos insertados
     */
    @Transactional
    public int insertarPedidosEnBatch(List<Pedido> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            return 0;
        }

        log.info("🚀 Iniciando batch insert de {} pedidos con productos en cascade...", pedidos.size());
        long startTime = System.currentTimeMillis();

        int insertados = 0;
        int lote = 0;

        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            
            // ✅ CRÍTICO: Asegurar relación bidireccional con productos
            if (pedido.getProductos() != null && !pedido.getProductos().isEmpty()) {
                for (Producto producto : pedido.getProductos()) {
                    // Si el producto no tiene referencia al pedido, agregarla
                    if (producto.getPedido() == null) {
                        producto.setPedido(pedido);
                    }
                }
            }

            // 🚀 CRÍTICO: persist() automáticamente persiste productos por CASCADE
            entityManager.persist(pedido);
            insertados++;

            // ⚠️ ESTRATEGIA CONSERVADORA: Solo flush() periódico, SIN clear()
            // Esto mantiene las entidades managed hasta el final de la transacción
            // Evita el error "delayed insert actions" por entidades desconectadas
            if ((i + 1) % batchSize == 0) {
                lote++;

                // Flush envía todo a BD pero mantiene las entidades en el contexto
                entityManager.flush();

                log.debug("  ✓ Lote {}: {} pedidos (+ productos) enviados a BD", lote, batchSize);
            }
        }

        // ✅ Flush final para pedidos restantes
        entityManager.flush();

        long endTime = System.currentTimeMillis();
        double segundos = (endTime - startTime) / 1000.0;
        
        log.info("✅ Batch insert completado: {} pedidos (+ productos en cascade) en {:.1f}s ({:.0f} pedidos/s)",
                insertados, segundos, insertados / segundos);

        // Clear solo al final de la transacción completa
        entityManager.clear();

        return insertados;
    }

    /**
     * 🔥 OPTIMIZADO: Inserta SOLO pedidos (sin productos en cascade)
     * Útil cuando quieres controlar el insert de productos por separado
     * 
     * @param pedidos Lista de pedidos (sin productos o con productos no persistidos)
     * @return Cantidad de pedidos insertados
     */
    @Transactional
    public int insertarSoloPedidosEnBatch(List<Pedido> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) {
            return 0;
        }

        log.info("🚀 Iniciando batch insert de {} pedidos (solo pedidos, sin productos)...", pedidos.size());
        long startTime = System.currentTimeMillis();

        int insertados = 0;

        for (int i = 0; i < pedidos.size(); i++) {
            Pedido pedido = pedidos.get(i);
            
            // Temporalmente remover productos para evitar cascade
            List<Producto> productosTemp = pedido.getProductos();
            pedido.setProductos(null);
            
            entityManager.persist(pedido);
            insertados++;
            
            // Restaurar productos (sin persistir aún)
            pedido.setProductos(productosTemp);

            if (i > 0 && i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();

        long endTime = System.currentTimeMillis();
        log.info("✅ Batch insert de pedidos completado: {} en {}ms", 
                insertados, (endTime - startTime));

        return insertados;
    }

    /**
     * 🔥 OPTIMIZADO: Inserta productos en batch
     * Útil después de insertar pedidos por separado
     * 
     * @param productos Lista de productos con pedido ya persistido
     * @return Cantidad de productos insertados
     */
    @Transactional
    public int insertarProductosEnBatch(List<Producto> productos) {
        if (productos == null || productos.isEmpty()) {
            return 0;
        }

        log.info("🚀 Iniciando batch insert de {} productos...", productos.size());
        long startTime = System.currentTimeMillis();

        int insertados = 0;

        for (int i = 0; i < productos.size(); i++) {
            Producto producto = productos.get(i);
            // 1) Re-enganchar el pedido como referencia managed
            if (producto.getPedido() != null && producto.getPedido().getId() != null) {
                Pedido pedidoRef = entityManager.getReference(Pedido.class, producto.getPedido().getId());
                producto.setPedido(pedidoRef);
            }

            // 2) merge() tolera transient o detached (persist() no)
            entityManager.merge(producto);
            //entityManager.persist(producto);
            insertados++;

            if (i > 0 && i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        entityManager.flush();
        entityManager.clear();

        long endTime = System.currentTimeMillis();
        log.info("✅ Batch insert de productos completado: {} en {}ms", 
                insertados, (endTime - startTime));

        return insertados;
    }

    /**
     * OPTIMIZADO: Inserta vuelos en batch
     * 
     * Perfecto para batch masivo
     * 
     * @param vuelos Lista de vuelos con aeropuertos ya persistidos
     * @return Cantidad de vuelos insertados
     */
    @Transactional
    public int insertarVuelosEnBatch(List<Vuelo> vuelos) {
        if (vuelos == null || vuelos.isEmpty()) {
            return 0;
        }

        log.info("🚀 Iniciando batch insert de {} vuelos...", vuelos.size());
        long startTime = System.currentTimeMillis();

        int insertados = 0;
        int lote = 0;

        for (int i = 0; i < vuelos.size(); i++) {
            Vuelo vuelo = vuelos.get(i);
            
            // CRÍTICO: persist() es más rápido que merge() para nuevas entidades
            entityManager.persist(vuelo);
            insertados++;

            // Flush y clear cada batch
            if (i > 0 && i % batchSize == 0) {
                lote++;
                entityManager.flush();
                entityManager.clear();
                
                log.debug("  ✓ Lote {}: {} vuelos persistidos", lote, batchSize);
            }
        }

        // Flush final
        entityManager.flush();
        entityManager.clear();

        long endTime = System.currentTimeMillis();
        double segundos = (endTime - startTime) / 1000.0;
        
        log.info("✅ Batch insert completado: {} vuelos en {:.2f}s ({:.0f} vuelos/s)",
                insertados, segundos, insertados / segundos);

        return insertados;
    }
}

