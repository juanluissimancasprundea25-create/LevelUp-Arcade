package com.leveluparcade.controller.web;

import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.Carrito;
import com.leveluparcade.entity.MetodoPago;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.CarritoService;
import com.leveluparcade.service.CheckoutService;
import com.leveluparcade.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Proceso de compra (checkout) para clientes logueados.
 *
 * <p>Rutas protegidas para ROLE_CLIENTE (cadena 3 de SecurityConfig).
 * CSRF deshabilitado en esa cadena, asi que los forms POST no necesitan
 * token CSRF.
 */
@Controller
public class CheckoutController {

    private final CarritoService carritoService;
    private final CheckoutService checkoutService;
    private final PedidoService pedidoService;
    private final SecurityHelper securityHelper;

    public CheckoutController(CarritoService carritoService,
                              CheckoutService checkoutService,
                              PedidoService pedidoService,
                              SecurityHelper securityHelper) {
        this.carritoService = carritoService;
        this.checkoutService = checkoutService;
        this.pedidoService = pedidoService;
        this.securityHelper = securityHelper;
    }

    /** Pantalla de checkout: resumen del carrito + formulario de envio/pago. */
    @GetMapping("/checkout")
    public String checkout(Model model, RedirectAttributes flash) {
        Carrito carrito = carritoService.obtenerCarritoActual();
        if (carrito.estaVacio()) {
            flash.addFlashAttribute("info", "Tu carrito esta vacio.");
            return "redirect:/carrito";
        }
        model.addAttribute("titulo", "Finalizar compra");
        model.addAttribute("carrito", carrito);
        model.addAttribute("metodosPago", MetodoPago.values());
        return "tienda/checkout";
    }

    /** Procesa el checkout: crea pedido pagado + factura y vacia el carrito. */
    @PostMapping("/checkout")
    public String procesar(@RequestParam("metodoPago") MetodoPago metodoPago,
                           @RequestParam(value = "direccionEnvio", required = false) String direccionEnvio,
                           RedirectAttributes flash) {
        try {
            PedidoResponse pedido = checkoutService.procesarCheckout(metodoPago, direccionEnvio);
            return "redirect:/checkout/confirmacion/" + pedido.id();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/carrito";
        }
    }

    /**
     * Pagina de confirmacion del pedido. Verifica que el pedido sea del
     * cliente logueado (PedidoService valida la propiedad por su lado;
     * aqui solo lo mostramos).
     */
    @GetMapping("/checkout/confirmacion/{pedidoId}")
    public String confirmacion(@PathVariable Long pedidoId,
                               Model model,
                               RedirectAttributes flash) {
        PedidoResponse pedido;
        try {
            pedido = pedidoService.obtenerPorId(pedidoId);
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", "No se ha encontrado el pedido.");
            return "redirect:/catalogo";
        }

        // Seguridad: un cliente solo puede ver SUS pedidos. Sin esto,
        // cambiando el id en la URL veria pedidos de otros clientes.
        Long clienteId = securityHelper.getClienteActualId();
        if (clienteId == null || !clienteId.equals(pedido.clienteId())) {
            flash.addFlashAttribute("error", "No tienes acceso a ese pedido.");
            return "redirect:/catalogo";
        }

        model.addAttribute("titulo", "Pedido confirmado");
        model.addAttribute("pedido", pedido);
        return "tienda/checkout-confirmacion";
    }
}
