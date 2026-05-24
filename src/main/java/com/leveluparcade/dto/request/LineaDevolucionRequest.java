package com.leveluparcade.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cada linea que el cliente quiere devolver: que linea del pedido
 * y cuantas unidades de ella.
 */
public class LineaDevolucionRequest {

    @NotNull(message = "El id de la linea de pedido es obligatorio")
    private Long lineaPedidoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    public LineaDevolucionRequest() {}

    public Long getLineaPedidoId() { return lineaPedidoId; }
    public void setLineaPedidoId(Long lineaPedidoId) { this.lineaPedidoId = lineaPedidoId; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}