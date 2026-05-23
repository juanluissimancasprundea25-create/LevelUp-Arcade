package com.leveluparcade.dto.response;

import com.leveluparcade.entity.LineaPedido;

import java.math.BigDecimal;

public record LineaPedidoResponse(
    Long id,
    Long productoId,
    String productoNombre,
    String productoSku,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal
) {

    public static LineaPedidoResponse from(LineaPedido l) {
        return new LineaPedidoResponse(
            l.getId(),
            l.getProducto().getId(),
            l.getProducto().getNombre(),
            l.getProducto().getSku(),
            l.getCantidad(),
            l.getPrecioUnitario(),
            l.getSubtotal()
        );
    }
}