package com.leveluparcade.auditoria;

/**
 * Tipos de eventos auditables del sistema.
 *
 * <p>Si necesitas anadir nuevos, simplemente extiende este enum. Cada
 * valor se guarda como string en {@code auditoria_log.accion}.
 */
public enum TipoEvento {

    // --- Autenticacion ---
    LOGIN_EXITO,
    LOGIN_FALLIDO,

    // --- Clientes ---
    CLIENTE_CREADO,
    CLIENTE_ACTUALIZADO,
    CLIENTE_ELIMINADO,

    // --- Proveedores ---
    PROVEEDOR_CREADO,
    PROVEEDOR_ACTUALIZADO,
    PROVEEDOR_ELIMINADO

    // Anadir aqui: PRODUCTO_*, PEDIDO_*, FACTURA_*, etc.
}