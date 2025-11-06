package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.model.Ciudad;
import com.grupo5e.morapack.repository.CiudadRepository;
import com.grupo5e.morapack.service.CiudadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CiudadServiceImpl implements CiudadService {

    private final CiudadRepository ciudadRepository;

    public CiudadServiceImpl(CiudadRepository ciudadRepository) {
        this.ciudadRepository = ciudadRepository;
    }

    @Override
    public List<Ciudad> listar() {
        return ciudadRepository.findAll();
    }

    @Override
    @Transactional
    public Integer insertar(Ciudad ciudad) {
        return ciudadRepository.save(ciudad).getId();
    }

    @Override
    @Transactional
    public Ciudad actualizar(Integer id, Ciudad ciudad) {
        Ciudad existente = buscarPorId(id);
        if (existente == null) {
            throw new ResourceNotFoundException("Ciudad", "id", id);
        }
        ciudad.setId(id);
        return ciudadRepository.save(ciudad);
    }

    @Override
    public Ciudad buscarPorId(Integer id) {
        return ciudadRepository.findById(id).orElse(null);
    }

    @Override
    public Optional<Ciudad> buscarPorCodigo(String codigo) {
        return ciudadRepository.findByCodigo(codigo);
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        if (!existePorId(id)) {
            throw new ResourceNotFoundException("Ciudad", "id", id);
        }
        ciudadRepository.deleteById(id);
    }

    @Override
    public boolean existePorId(Integer id) {
        return ciudadRepository.existsById(id);
    }

    @Override
    @Transactional
    public List<Ciudad> insertarBulk(List<Ciudad> ciudades) {
        return ciudadRepository.saveAll(ciudades).stream().collect(Collectors.toList());
    }
}
