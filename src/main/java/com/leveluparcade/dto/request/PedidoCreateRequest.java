package com.leveluparcade.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datos de entrada para crear un nuevo pedido.
 *
 * <p>{@code clienteId} se ignora si quien llama es un CLIENTE (se
 * infiere del usuario logueado). Solo un ADMIN puede crear pedidos
 * en nombre de otro cliente, en cuyo caso es obligatorio.
 */
public record PedidoCreateRequest(

    /** Solo lo usa el ADMIN. Si lo manda un CLIENTE se ignora. */
    Long clienteId,

    @NotEmpty(message = "Un pedido debe tener al menos una linea")
    @Valid
    List<LineaPedidoRequest> lineas,

    @Size(max = 255)
    String direccionEnvio
) {}