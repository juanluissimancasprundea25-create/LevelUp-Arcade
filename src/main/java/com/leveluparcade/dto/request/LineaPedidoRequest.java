package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Linea individual dentro de un PedidoCreateRequest.
 *
 * <p>Solo lleva el id del producto y la cantidad. El precio se obtiene
 * del producto en el service en el momento del pedido (snapshot).
 */
public record LineaPedidoRequest(

    @NotNull(message = "El productoId es obligatorio")
    Long productoId,

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor que 0")
    Integer cantidad
) {}