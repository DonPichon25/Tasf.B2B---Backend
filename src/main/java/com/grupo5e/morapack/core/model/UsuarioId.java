package com.grupo5e.morapack.core.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UsuarioId implements Serializable {

    private Long id;       // ID que viene del archivo
    private int tipoData;  // 0 = prueba, 1 = real

}
