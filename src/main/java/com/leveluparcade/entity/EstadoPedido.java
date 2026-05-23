package com.leveluparcade.entity;

/**
 * Estados posibles de un pedido.
 *
 * <p>Transiciones permitidas:
 * <pre>
 *   PENDIENTE → PAGADO → ENVIADO → ENTREGADO
 *       ↓         ↓
 *       └────→ CANCELADO ←────┘
 * </pre>
 *
 * <p>Las transiciones se validan en {@code PedidoService}, no a nivel
 * de constraint de BD. La BD solo valida que el valor sea uno de los
 * permitidos (CHECK en V1__init.sql).
 */
public enum EstadoPedido {
    PENDIENTE,
    PAGADO,
    ENVIADO,
    ENTREGADO,
    CANCELADO
}