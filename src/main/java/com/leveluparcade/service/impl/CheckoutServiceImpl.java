package com.leveluparcade.service.impl;

import com.leveluparcade.dto.request.EmitirFacturaRequest;
import com.leveluparcade.dto.request.LineaPedidoRequest;
import com.leveluparcade.dto.request.PagarPedidoRequest;
import com.leveluparcade.dto.request.PedidoCreateRequest;
import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.Carrito;
import com.leveluparcade.entity.MetodoPago;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.CarritoService;
import com.leveluparcade.service.CheckoutService;
import com.leveluparcade.service.FacturaService;
import com.leveluparcade.service.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion de {@link CheckoutService}.
 *
 * <p>Reutiliza PedidoService (Simancas) y FacturaService (Simancas) sin
 * duplicar logica de stock ni de facturacion. Todo el flujo va en una
 * unica transaccion: si algo falla (p.ej. stock agotado al pagar), se
 * revierte completo y el carrito NO se vacia.
 */
@Service
public class CheckoutServiceImpl implements CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutServiceImpl.class);

    private final CarritoService carritoService;
    private final PedidoService pedidoService;
    private final FacturaService facturaService;
    private final SecurityHelper securityHelper;

    public CheckoutServiceImpl(CarritoService carritoService,
                               PedidoService pedidoService,
                               FacturaService facturaService,
                               SecurityHelper securityHelper) {
        this.carritoService = carritoService;
        this.pedidoService = pedidoService;
        this.facturaService = facturaService;
        this.securityHelper = securityHelper;
    }

    @Override
    @Transactional
    public PedidoResponse procesarCheckout(MetodoPago metodoPago, String direccionEnvio) {
        Long clienteId = securityHelper.getClienteActualId();
        if (clienteId == null) {
            throw new IllegalStateException("No hay un cliente autenticado.");
        }

        Carrito carrito = carritoService.obtenerCarritoActual();
        if (carrito.estaVacio()) {
            throw new IllegalStateException("Tu carrito esta vacio.");
        }

        // 1) Construir el request de pedido a partir de las lineas del carrito.
        List<LineaPedidoRequest> lineas = carrito.getLineas().stream()
                .map(l -> new LineaPedidoRequest(l.getProducto().getId(), l.getCantidad()))
                .toList();

        PedidoCreateRequest crearReq = new PedidoCreateRequest(clienteId, lineas, direccionEnvio);

        // 2) Crear el pedido (PENDIENTE): valida stock y congela precios.
        PedidoResponse pedido = pedidoService.crear(clienteId, crearReq);

        // 3) Pagar (PAGADO): descuenta stock. Re-valida stock dentro.
        PagarPedidoRequest pagarReq = new PagarPedidoRequest(metodoPago);
        pedido = pedidoService.pagar(pedido.id(), pagarReq);

        // 4) Emitir factura (idempotente: si ya existiera, la devuelve).
        EmitirFacturaRequest facturaReq = new EmitirFacturaRequest();
        facturaReq.setPedidoId(pedido.id());
        facturaService.emitirFactura(facturaReq);

        // 5) Vaciar el carrito (solo si todo lo anterior fue bien).
        carritoService.vaciar();

        log.info(">>> Checkout OK: cliente={}, pedido={}, total={}, metodo={}",
                clienteId, pedido.id(), pedido.total(), metodoPago);

        return pedido;
    }
}
