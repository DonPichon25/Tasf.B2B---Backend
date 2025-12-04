package com.grupo5e.morapack.repository;

import com.grupo5e.morapack.core.model.InstanciaVuelo;
import com.grupo5e.morapack.core.model.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para gestionar instancias de vuelo.
 */
@Repository
public interface InstanciaVueloRepository extends JpaRepository<InstanciaVuelo, String> {

    /**
     * Encuentra todas las instancias de un vuelo base específico.
     */
    List<InstanciaVuelo> findByVueloBase(Vuelo vueloBase);

    /**
     * Encuentra todas las instancias de un vuelo base por ID.
     */
    List<InstanciaVuelo> findByVueloBaseId(Integer vueloBaseId);

    /**
     * Encuentra instancias que salen dentro de una ventana de tiempo.
     */
    @Query("SELECT i FROM InstanciaVuelo i WHERE i.fechaHoraSalida >= :inicio AND i.fechaHoraSalida <= :fin")
    List<InstanciaVuelo> findByFechaHoraSalidaBetween(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    /**
     * Encuentra instancias con capacidad disponible para una cantidad específica.
     */
    @Query("SELECT i FROM InstanciaVuelo i WHERE (i.capacidadMaxima - i.capacidadUsada) >= :cantidadRequerida")
    List<InstanciaVuelo> findByCapacidadDisponibleGreaterThanEqual(@Param("cantidadRequerida") int cantidadRequerida);

    /**
     * Encuentra instancias de un vuelo específico con capacidad disponible en una
     * ventana de tiempo.
     */
    @Query("SELECT i FROM InstanciaVuelo i " +
            "WHERE i.vueloBase.id = :vueloId " +
            "AND i.fechaHoraSalida >= :inicio " +
            "AND i.fechaHoraSalida <= :fin " +
            "AND (i.capacidadMaxima - i.capacidadUsada) >= :cantidadRequerida " +
            "ORDER BY i.fechaHoraSalida ASC")
    List<InstanciaVuelo> findAvailableInstancesForFlight(
            @Param("vueloId") Integer vueloId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("cantidadRequerida") int cantidadRequerida);
}
