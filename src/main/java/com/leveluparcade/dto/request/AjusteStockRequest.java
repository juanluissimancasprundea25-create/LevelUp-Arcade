package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ajuste manual de stock (entrada o salida).
 *
 * <p>{@code cantidad} positiva = entrada (entrada de mercancia).
 * {@code cantidad} negativa = salida (baja manual, rotura, etc).
 * El motivo se guarda en el log de auditoria.
 */
public record AjusteStockRequest(

    @NotNull(message = "La cantidad es obligatoria")
    Integer cantidad,

    @Size(max = 500)
    String motivo

) {}