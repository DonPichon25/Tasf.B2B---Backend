package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.model.Ruta;
import com.grupo5e.morapack.repository.RutaRepository;
import com.grupo5e.morapack.service.RutaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RutaServiceImpl implements RutaService {

    private final RutaRepository rutaRepository;

    public RutaServiceImpl(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    @Override
    public List<Ruta> listar() {
        return rutaRepository.findAll();
    }

    @Override
    @Transactional
    public Integer insertar(Ruta ruta) {
        return rutaRepository.save(ruta).getId();
    }

    @Override
    @Transactional
    public Ruta actualizar(Integer id, Ruta ruta) {
        Ruta existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Ruta", "id", id);
        }
        ruta.setId(id);
        return rutaRepository.save(ruta);
    }

    @Override
    public Ruta buscarPorId(Integer id) {
        return rutaRepository.findById(id).orElse(null);
    }

    @Override
    public List<Ruta> buscarPorAeropuertoOrigen(Integer aeropuertoId) {
        return rutaRepository.findByAeropuertoOrigenId(aeropuertoId);
    }

    @Override
    public List<Ruta> buscarPorAeropuertoDestino(Integer aeropuertoId) {
        return rutaRepository.findByAeropuertoDestinoId(aeropuertoId);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Ruta", "id", id);
        }
        rutaRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Integer id) {
        return rutaRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Ruta> insertarBulk(List<Ruta> rutas) {
        return rutaRepository.saveAll(rutas).stream().collect(Collectors.toList());
    }
    
    // Nuevos métodos siguiendo patrón Backend
    
    @Override
    public List<Ruta> buscarPorSolucionId(Integer solucionId) {
        return rutaRepository.findBySolucionId(solucionId);
    }
    
    @Override
    public List<Ruta> buscarPorVueloId(Integer vueloId) {
        return rutaRepository.findByVueloId(vueloId);
    }
    
    @Override
    public List<Ruta> buscarPorPedidoId(Integer pedidoId) {
        return rutaRepository.findByPedidoId(pedidoId);
    }
    
    @Override
    public List<Ruta> buscarPorRangoTiempo(Double min, Double max) {
        return rutaRepository.findByTiempoTotalBetween(min, max);
    }
    
    @Override
    public List<Ruta> buscarPorRangoCosto(Double min, Double max) {
        return rutaRepository.findByCostoTotalBetween(min, max);
    }
    
    @Override
    public List<Ruta> buscarPorOrigenYDestino(Integer origenId, Integer destinoId) {
        return rutaRepository.findByOrigenAndDestino(origenId, destinoId);
    }
}
