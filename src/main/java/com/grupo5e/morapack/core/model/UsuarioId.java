package com.grupo5e.morapack.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UsuarioId implements Serializable {

//    private Long id;       // ID que viene del archivo
//    private int tipoData;  // 0 = prueba, 1 = real
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_data", columnDefinition = "int default 1")
    private Integer tipoData;

    // IMPORTANTE: Implementar equals() y hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsuarioId)) return false;
        UsuarioId that = (UsuarioId) o;
        return Objects.equals(id, that.id) && Objects.equals(tipoData, that.tipoData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tipoData);
    }
}
