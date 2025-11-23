package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.enums.EstadoAeropuerto;
import com.grupo5e.morapack.core.model.Aeropuerto;
import com.grupo5e.morapack.repository.AeropuertoRepository;
import com.grupo5e.morapack.service.AeropuertoService;
import jakarta.persistence.EntityManager;
import org.hibernate.annotations.processing.SQL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AeropuertoServiceImpl implements AeropuertoService {

    private final AeropuertoRepository aeropuertoRepository;
    @Autowired
    private EntityManager entityManager;

    public AeropuertoServiceImpl(AeropuertoRepository aeropuertoRepository) {
        this.aeropuertoRepository = aeropuertoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aeropuerto> listar() {
        List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
        // Forzar inicialización de relaciones LAZY (Almacen y Ciudad)
        aeropuertos.forEach(a -> {
            if (a.getAlmacen() != null) {
                a.getAlmacen().getCapacidadUsada();
            }
            if (a.getCiudad() != null) {
                a.getCiudad().getCodigo(); // Fuerza inicialización de Ciudad
            }
        });
        return aeropuertos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aeropuerto> listarDisponibles() {
        List<Aeropuerto> aeropuertos = aeropuertoRepository.findByEstado(EstadoAeropuerto.DISPONIBLE);
        // Forzar inicialización de relaciones LAZY (Almacen y Ciudad)
        aeropuertos.forEach(a -> {
            if (a.getAlmacen() != null) {
                a.getAlmacen().getCapacidadUsada();
            }
            if (a.getCiudad() != null) {
                a.getCiudad().getCodigo(); // Fuerza inicialización de Ciudad
            }
        });
        return aeropuertos;
    }

    @Override
    @Transactional
    public Integer insertar(Aeropuerto aeropuerto) {
        return aeropuertoRepository.save(aeropuerto).getId();
    }

    @Override
    @Transactional
    public Aeropuerto actualizar(Integer id, Aeropuerto aeropuerto) {
        Aeropuerto existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Aeropuerto", "id", id);
        }
        aeropuerto.setId(id);
        return aeropuertoRepository.save(aeropuerto);
    }

    @Override
    @Transactional
    public Aeropuerto toggleEstado(Integer id) {
        Aeropuerto aeropuerto = buscarPorId(id);
        if (aeropuerto == null) {
            throw new ResourceNotFoundException("Aeropuerto", "id", id);
        }
        
        // Cambiar estado: DISPONIBLE ↔ NO_DISPONIBLE
        if (aeropuerto.getEstado() == EstadoAeropuerto.DISPONIBLE) {
            aeropuerto.setEstado(EstadoAeropuerto.NO_DISPONIBLE);
        } else {
            aeropuerto.setEstado(EstadoAeropuerto.DISPONIBLE);
        }
        
        return aeropuertoRepository.save(aeropuerto);
    }

    @Override
    @Transactional(readOnly = true)
    public Aeropuerto buscarPorId(Integer id) {
        Aeropuerto aeropuerto = aeropuertoRepository.findById(id).orElse(null);
        if (aeropuerto != null) {
            // Forzar inicialización de relaciones LAZY
            if (aeropuerto.getAlmacen() != null) {
                aeropuerto.getAlmacen().getCapacidadUsada();
            }
            if (aeropuerto.getCiudad() != null) {
                aeropuerto.getCiudad().getCodigo();
            }
        }
        return aeropuerto;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Aeropuerto> buscarPorCodigoIATA(String codigoIATA) {
        Optional<Aeropuerto> aeropuerto = aeropuertoRepository.findByCodigoIATA(codigoIATA);
        aeropuerto.ifPresent(a -> {
            // Forzar inicialización de relaciones LAZY
            if (a.getAlmacen() != null) {
                a.getAlmacen().getCapacidadUsada();
            }
            if (a.getCiudad() != null) {
                a.getCiudad().getCodigo();
            }
        });
        return aeropuerto;
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Aeropuerto", "id", id);
        }
        aeropuertoRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Integer id) {
        return aeropuertoRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Aeropuerto> insertarBulk(List<Aeropuerto> aeropuertos) {
        return aeropuertoRepository.saveAll(aeropuertos).stream().collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Aeropuerto> listartipoData(int tipoData) {
        return aeropuertoRepository.listarPorTipoData(tipoData);
    }

    @Override
    @Transactional
    public void limpiarBD() {
        aeropuertoRepository.eliminarTipoDataCero();
    }

    @Override
    public int contarTipoDataCero() {
        return aeropuertoRepository.contarTipoDataCero();
    }

}
