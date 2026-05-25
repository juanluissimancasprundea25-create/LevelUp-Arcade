package com.leveluparcade.dto.response;

import com.leveluparcade.entity.Categoria;

/** Representacion de una Categoria en respuestas de la API. */
public record CategoriaResponse(
    Long id,
    String nombre,
    String descripcion,
    Boolean activa
) {

    public static CategoriaResponse from(Categoria c) {
        return new CategoriaResponse(
            c.getId(),
            c.getNombre(),
            c.getDescripcion(),
            c.getActiva()
        );
    }
}