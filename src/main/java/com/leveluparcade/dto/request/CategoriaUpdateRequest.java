package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para actualizar una categoria existente.
 *
 * <p>Permite cambiar nombre (validando unicidad), descripcion y
 * estado activa/inactiva.
 */
public record CategoriaUpdateRequest(

    @NotBlank(message = "El nombre de la categoria es obligatorio")
    @Size(max = 100)
    String nombre,

    @Size(max = 2000, message = "La descripcion no puede exceder 2000 caracteres")
    String descripcion,

    Boolean activa

) {}