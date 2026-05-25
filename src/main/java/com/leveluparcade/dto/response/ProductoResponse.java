package com.leveluparcade.dto.response;

import com.leveluparcade.entity.Producto;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representacion de un Producto en respuestas de la API. */
public record ProductoResponse(
    Long id,
    String sku,
    String nombre,
    String descripcion,
    BigDecimal precio,
    Integer stock,
    Integer stockMinimo,
    Long categoriaId,
    String categoriaNombre,
    Long proveedorId,
    String proveedorNombre,
    String imagenUrl,
    Boolean activo,
    Boolean bajoStock,
    LocalDateTime fechaAlta,
    LocalDateTime fechaActualizacion
) {

    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
            p.getId(),
            p.getSku(),
            p.getNombre(),
            p.getDescripcion(),
            p.getPrecio(),
            p.getStock(),
            p.getStockMinimo(),
            p.getCategoria() != null ? p.getCategoria().getId() : null,
            p.getCategoria() != null ? p.getCategoria().getNombre() : null,
            p.getProveedor() != null ? p.getProveedor().getId() : null,
            p.getProveedor() != null ? p.getProveedor().getNombreEmpresa() : null,
            p.getImagenUrl(),
            p.getActivo(),
            p.getStock() != null && p.getStockMinimo() != null
                && p.getStock() <= p.getStockMinimo(),
            p.getFechaAlta(),
            p.getFechaActualizacion()
        );
    }
}