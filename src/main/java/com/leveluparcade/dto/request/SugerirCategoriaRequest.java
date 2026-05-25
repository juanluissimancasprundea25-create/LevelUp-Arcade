package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Peticion para que la IA sugiera una categoria para un producto.
 *
 * <p>Las categorias existentes se consultan en BD; no las envia el cliente.
 */
public record SugerirCategoriaRequest(

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 200)
    String nombreProducto,

    @Size(max = 5000)
    String descripcion

) {}