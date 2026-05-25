package com.leveluparcade.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos de entrada para actualizar un producto existente.
 *
 * <p>Permite cambiar todos los campos incluido el SKU (con validacion de
 * unicidad en el service) y activar/desactivar el producto.
 */
public record ProductoUpdateRequest(

    @NotBlank(message = "El SKU es obligatorio")
    @Pattern(
        regexp = "^[A-Z0-9-]{3,50}$",
        message = "El SKU debe contener solo letras mayusculas, digitos y guiones (3-50 caracteres)"
    )
    @Size(max = 50)
    String sku,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200)
    String nombre,

    @Size(max = 5000)
    String descripcion,

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
    BigDecimal precio,

    @NotNull(message = "El stock es obligatorio")
    @PositiveOrZero(message = "El stock no puede ser negativo")
    Integer stock,

    @PositiveOrZero(message = "El stock minimo no puede ser negativo")
    Integer stockMinimo,

    Long categoriaId,

    Long proveedorId,

    @Size(max = 500)
    String imagenUrl,

    Boolean activo

) {}