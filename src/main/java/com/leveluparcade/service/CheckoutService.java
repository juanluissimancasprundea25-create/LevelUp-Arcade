package com.leveluparcade.service;

import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.MetodoPago;

/**
 * Orquesta el proceso de compra (checkout) del cliente autenticado.
 *
 * <p>NO duplica logica de pedidos/stock/facturas: reutiliza
 * {@link PedidoService} (crear + pagar) y {@link FacturaService}
 * (emitir). Su unica responsabilidad es coordinar el flujo carrito ->
 * pedido pagado + factura + vaciar carrito, todo en una transaccion.
 */
public interface CheckoutService {

    /**
     * Procesa el checkout del cliente logueado:
     * <ol>
     *   <li>Lee su carrito (debe tener lineas).</li>
     *   <li>Crea un Pedido a partir de las lineas del carrito.</li>
     *   <li>Lo marca como PAGADO con el metodo indicado (descuenta stock).</li>
     *   <li>Emite la factura del pedido (idempotente).</li>
     *   <li>Vacia el carrito.</li>
     * </ol>
     *
     * <p>Como es un proyecto academico sin pasarela real, el pago se
     * considera confirmado en el momento (crear + pagar en un paso).
     *
     * @param metodoPago      metodo de pago elegido
     * @param direccionEnvio  direccion de envio (texto libre)
     * @return el pedido resultante (ya PAGADO)
     * @throws IllegalStateException    si el carrito esta vacio
     * @throws IllegalArgumentException si algun producto se quedo sin stock
     */
    PedidoResponse procesarCheckout(MetodoPago metodoPago, String direccionEnvio);
}
