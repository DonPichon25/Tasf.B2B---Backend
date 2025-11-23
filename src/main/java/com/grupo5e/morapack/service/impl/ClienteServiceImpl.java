package com.grupo5e.morapack.service.impl;

import com.grupo5e.morapack.api.exception.ResourceNotFoundException;
import com.grupo5e.morapack.core.model.Cliente;
import com.grupo5e.morapack.core.model.UsuarioId;
import com.grupo5e.morapack.repository.ClienteRepository;
import com.grupo5e.morapack.service.ClienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

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
        return clienteRepository.saveAll(clientes).stream().collect(Collectors.toList());
    }
}
