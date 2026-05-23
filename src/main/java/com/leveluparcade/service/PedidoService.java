package com.leveluparcade.service;

import com.leveluparcade.dto.request.PagarPedidoRequest;
import com.leveluparcade.dto.request.PedidoCreateRequest;
import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.EstadoPedido;

import java.util.List;

/**
 * Operaciones de gestion de pedidos.
 *
 * <p>El ciclo de vida de un pedido es:
 * PENDIENTE -> PAGADO -> ENVIADO -> ENTREGADO, con posibilidad de
 * CANCELAR desde PENDIENTE o PAGADO. Cada transicion tiene su propio
 * metodo en lugar de un setEstado generico, para garantizar
 * invariantes (validar estado previo, descontar/restaurar stock, etc.).
 */
public interface PedidoService {

    /** Todos los pedidos (solo admin). */
    List<PedidoResponse> listarTodos();

    /** Pedidos por estado. */
    List<PedidoResponse> listarPorEstado(EstadoPedido estado);

    /** Pedidos de un cliente concreto. */
    List<PedidoResponse> listarDeCliente(Long clienteId);

    /** Obtiene un pedido por id. */
    PedidoResponse obtenerPorId(Long id);

    /**
     * Crea un nuevo pedido en estado PENDIENTE.
     *
     * <p>Valida que cada producto tenga stock suficiente para la cantidad
     * pedida (pero NO descuenta el stock; eso pasa al pagar). El precio
     * unitario de cada linea se copia del producto en este momento.
     *
     * @throws IllegalArgumentException si algun producto no existe o no hay stock
     */
    PedidoResponse crear(Long clienteId, PedidoCreateRequest request);

    /**
     * Marca el pedido como PAGADO y descuenta el stock de cada producto.
     * Solo permitido desde estado PENDIENTE.
     *
     * @throws IllegalStateException si el pedido no esta PENDIENTE
     * @throws IllegalStateException si entre la creacion y el pago el stock se ha agotado
     */
    PedidoResponse pagar(Long id, PagarPedidoRequest request);

    /** Marca el pedido como ENVIADO. Solo desde PAGADO. */
    PedidoResponse enviar(Long id);

    /** Marca el pedido como ENTREGADO. Solo desde ENVIADO. */
    PedidoResponse entregar(Long id);

    /**
     * Cancela el pedido. Solo permitido desde PENDIENTE o PAGADO.
     * Si estaba PAGADO, restaura el stock de los productos.
     */
    PedidoResponse cancelar(Long id);
}