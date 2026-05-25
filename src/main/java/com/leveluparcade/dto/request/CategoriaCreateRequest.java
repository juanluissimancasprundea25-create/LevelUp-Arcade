package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para crear una nueva categoria.
 *
 * <p>{@code nombre} es obligatorio y unico. {@code descripcion} es opcional.
 */
public record CategoriaCreateRequest(

    @NotBlank(message = "El nombre de la categoria es obligatorio")
    @Size(max = 100)
    String nombre,

    @Size(max = 2000, message = "La descripcion no puede exceder 2000 caracteres")
    String descripcion

) {}