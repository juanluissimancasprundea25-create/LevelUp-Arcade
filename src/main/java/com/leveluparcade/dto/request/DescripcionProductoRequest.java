package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Peticion para generar una descripcion de producto con IA.
 *
 * <p>{@code categoria} es opcional pero mejora la calidad del resultado.
 */
public record DescripcionProductoRequest(

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 200)
    String nombreProducto,

    @Size(max = 100)
    String categoria

) {}