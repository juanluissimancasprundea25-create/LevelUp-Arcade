package com.leveluparcade.dto.response;

import com.leveluparcade.entity.EstadoPedido;
import com.leveluparcade.entity.MetodoPago;
import com.leveluparcade.entity.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
    Long id,
    Long clienteId,
    String clienteEmail,
    String clienteNombreCompleto,
    LocalDateTime fechaPedido,
    EstadoPedido estado,
    BigDecimal total,
    MetodoPago metodoPago,
    String direccionEnvio,
    List<LineaPedidoResponse> lineas
) {

    public static PedidoResponse from(Pedido p) {
        return new PedidoResponse(
            p.getId(),
            p.getCliente().getId(),
            p.getCliente().getUsuario().getEmail(),
            p.getCliente().getUsuario().getNombreCompleto(),
            p.getFechaPedido(),
            p.getEstado(),
            p.getTotal(),
            p.getMetodoPago(),
            p.getDireccionEnvio(),
            p.getLineas().stream()
                .map(LineaPedidoResponse::from)
                .toList()
        );
    }
}