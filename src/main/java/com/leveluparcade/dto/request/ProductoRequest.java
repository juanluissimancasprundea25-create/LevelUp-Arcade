package com.leveluparcade.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductoRequest {

    @NotBlank
    private String sku;

    @NotBlank
    private String nombre;

    private String descripcion;

    @NotNull
    @PositiveOrZero
    private BigDecimal precio;

    @NotNull
    @PositiveOrZero
    private Integer stock;

    @NotNull
    private Integer stockMinimo;

    private Long categoriaId;

    private String imagenUrl;

    private Boolean activo;
}