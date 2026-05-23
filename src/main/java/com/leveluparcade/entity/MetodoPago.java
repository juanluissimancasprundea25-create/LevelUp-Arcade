package com.leveluparcade.entity;

/**
 * Metodos de pago aceptados.
 *
 * <p>Restringido a los valores definidos en el CHECK de la tabla pedidos
 * en V1__init.sql.
 */
public enum MetodoPago {
    TARJETA,
    PAYPAL,
    TRANSFERENCIA
}