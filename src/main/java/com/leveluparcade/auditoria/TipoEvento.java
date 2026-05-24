package com.leveluparcade.auditoria;

/**
 * Catalogo de eventos auditables del sistema.
 *
 * Convencion: {ENTIDAD}_{ACCION}.
 * Anadir aqui cualquier evento nuevo y publicarlo desde el service.
 */
public enum TipoEvento {

    // === Autenticacion ===
    LOGIN_EXITO,
    LOGIN_FALLIDO,

    // === Clientes ===
    CLIENTE_CREADO,
    CLIENTE_ACTUALIZADO,
    CLIENTE_ELIMINADO,

    // === Proveedores ===
    PROVEEDOR_CREADO,
    PROVEEDOR_ACTUALIZADO,
    PROVEEDOR_ELIMINADO,

    // === Pedidos ===
    PEDIDO_CREADO,
    PEDIDO_PAGADO,
    PEDIDO_ENVIADO,
    PEDIDO_ENTREGADO,
    PEDIDO_CANCELADO,

    // === Devoluciones ===
    DEVOLUCION_SOLICITADA,
    DEVOLUCION_APROBADA,
    DEVOLUCION_RECHAZADA,
    DEVOLUCION_COMPLETADA,

    // === Facturas ===
    FACTURA_EMITIDA,
    FACTURA_DESCARGADA,

    // === Chat ===
    MENSAJE_ENVIADO,
    MENSAJE_LEIDO

}