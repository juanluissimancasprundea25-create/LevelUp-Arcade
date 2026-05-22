package com.leveluparcade.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductoResponse {

    private Long id;
    private String sku;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private Integer stock;
    private Integer stockMinimo;
    private Boolean activo;
    private String categoriaNombre;
}