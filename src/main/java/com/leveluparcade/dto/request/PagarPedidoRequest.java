package com.leveluparcade.dto.request;

import com.leveluparcade.entity.MetodoPago;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para marcar un pedido como pagado.
 */
public record PagarPedidoRequest(

    @NotNull(message = "El metodo de pago es obligatorio")
    MetodoPago metodoPago
) {}