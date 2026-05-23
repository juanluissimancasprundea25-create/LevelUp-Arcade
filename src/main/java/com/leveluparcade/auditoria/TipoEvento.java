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
    PROVEEDOR_ELIMINADO,

    // --- Pedidos ---
    PEDIDO_CREADO,
    PEDIDO_PAGADO,
    PEDIDO_ENVIADO,
    PEDIDO_ENTREGADO,
    PEDIDO_CANCELADO

    // Anadir aqui: FACTURA_*, DEVOLUCION_*, etc.
}