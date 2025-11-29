package com.grupo5e.morapack.service;

import com.grupo5e.morapack.core.model.Empleado;
import com.grupo5e.morapack.core.enums.Rol;
import com.grupo5e.morapack.core.model.UsuarioId;

import java.util.List;
import java.util.Optional;

public interface EmpleadoService {
    List<Empleado> listar();
    UsuarioId insertar(Empleado empleado);
    Empleado actualizar(UsuarioId id, Empleado empleado);
    Empleado buscarPorId(UsuarioId id);
    List<Empleado> buscarPorRol(Rol rol);
    Optional<Empleado> buscarPorUsername(String username);
    void eliminar(UsuarioId id);
    boolean existePorId(UsuarioId id);
    List<Empleado> insertarBulk(List<Empleado> empleados);
}
