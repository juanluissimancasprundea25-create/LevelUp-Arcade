package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotNull;

public class EmitirFacturaRequest {

    @NotNull(message = "pedidoId es obligatorio")
    private Long pedidoId;

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }
}