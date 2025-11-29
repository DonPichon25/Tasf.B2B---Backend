package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.model.Cliente;
import com.grupo5e.morapack.core.model.UsuarioId;
import com.grupo5e.morapack.repository.ClienteRepository;
import com.grupo5e.morapack.service.ClienteService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final EntityManager entityManager;

    @Override
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    @Override
    @Transactional
    public Long insertar(Cliente cliente) {
        return clienteRepository.save(cliente).getUsuarioId().getId();
    }

    @Override
    @Transactional
    public Cliente actualizar(Long id, int tipoData,Cliente cliente) {
        UsuarioId pk = new UsuarioId(id, tipoData);
        Cliente existente = buscarPorId(id,tipoData);
        if (existente == null) {
            throw new ResourceNotFoundException("Cliente", "id", id);
        }
        cliente.setUsuarioId(pk);
        return clienteRepository.save(cliente);
    }

    @Override
    public Cliente buscarPorId(Long idCliente,int tipoData) {
        UsuarioId pk = new UsuarioId(idCliente, tipoData);
        return clienteRepository.findById(pk).orElse(null);
    }

    @Override
    public Optional<Cliente> findByNumeroDocumento(String numeroDocumento) {
        return clienteRepository.findByNumeroDocumento(numeroDocumento);
    }

    @Override
    public Optional<Cliente> findByCorreo(String correo) {
        return clienteRepository.findByCorreo(correo);
    }

    @Override
    @Transactional
    public void eliminar(Long id,int tipoData) {
        UsuarioId pk = new UsuarioId(id, tipoData);
        if (!existePorId(id,tipoData)) {
            throw new ResourceNotFoundException("Cliente", "id", id);
        }
        clienteRepository.deleteById(pk);
    }

    @Override
    public boolean existePorId(Long id,int tipoData) {
        UsuarioId pk = new UsuarioId(id, tipoData);
        return clienteRepository.existsById(pk);
    }

    @Override
    @Transactional
    public List<Cliente> insertarBulk(List<Cliente> clientes) {
        //return clienteRepository.saveAll(clientes).stream().collect(Collectors.toList());
        List<Cliente> result = new ArrayList<>();
        int batchSize = 1000;

        for (int i = 0; i < clientes.size(); i++) {
            entityManager.persist(clientes.get(i));
            result.add(clientes.get(i));
            //System.out.println("Inserted Cliente with ID: " + clientes.get(i).getUsuarioId().getId());
            if (i % batchSize == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        // último flush
        entityManager.flush();
        entityManager.clear();

        return result;
    }

    @Override
    public List<Cliente> listarPorTipoData(int tipoData) {
        return clienteRepository.findAllByTipoData(tipoData);
    }
}
