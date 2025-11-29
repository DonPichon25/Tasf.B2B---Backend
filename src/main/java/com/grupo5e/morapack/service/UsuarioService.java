package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Usuario;
import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.model.UsuarioId;

import java.util.List;
import java.util.Optional;

public interface UsuarioService {
    List<Usuario> listar();
    UsuarioId insertar(Usuario usuario);
    Usuario actualizar(UsuarioId id, Usuario usuario);
    Usuario buscarPorId(UsuarioId id);
    Optional<Usuario> buscarPorUsername(String username);
    List<Usuario> buscarPorRol(Rol rol);
    void eliminar(UsuarioId id);
    boolean existePorId(UsuarioId id);
}
