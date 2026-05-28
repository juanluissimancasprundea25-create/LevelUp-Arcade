package com.leveluparcade.controller.web;

import com.leveluparcade.entity.Carrito;
import com.leveluparcade.service.CarritoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Carrito de compra del cliente. Todas las rutas estan protegidas para
 * ROLE_CLIENTE (cadena 3 de SecurityConfig); un anonimo es redirigido a
 * /login automaticamente por el form-login.
 *
 * <p>CSRF esta deshabilitado en esa cadena, por lo que los forms POST de
 * carrito NO necesitan token CSRF.
 */
@Controller
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    /** Pagina del carrito: listado de lineas + total. */
    @GetMapping("/carrito")
    public String ver(Model model) {
        Carrito carrito = carritoService.obtenerCarritoActual();
        model.addAttribute("titulo", "Mi carrito");
        model.addAttribute("carrito", carrito);
        return "tienda/carrito";
    }

    /** Anade un producto al carrito (desde el detalle de producto). */
    @PostMapping("/carrito/add")
    public String anadir(@RequestParam("productoId") Long productoId,
                         @RequestParam(value = "cantidad", required = false, defaultValue = "1") Integer cantidad,
                         RedirectAttributes flash) {
        try {
            carritoService.anadirProducto(productoId, cantidad != null ? cantidad : 1);
            flash.addFlashAttribute("info", "Producto anadido al carrito.");
            return "redirect:/carrito";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/producto/" + productoId;
        }
    }

    /** Cambia la cantidad de una linea. Cantidad 0 elimina la linea. */
    @PostMapping("/carrito/linea/{lineaId}")
    public String cambiar(@PathVariable Long lineaId,
                          @RequestParam("cantidad") int cantidad,
                          RedirectAttributes flash) {
        try {
            carritoService.cambiarCantidad(lineaId, cantidad);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/carrito";
    }

    /** Elimina una linea del carrito. */
    @PostMapping("/carrito/linea/{lineaId}/eliminar")
    public String eliminar(@PathVariable Long lineaId, RedirectAttributes flash) {
        carritoService.eliminarLinea(lineaId);
        flash.addFlashAttribute("info", "Producto eliminado del carrito.");
        return "redirect:/carrito";
    }

    /** Vacia el carrito por completo. */
    @PostMapping("/carrito/vaciar")
    public String vaciar(RedirectAttributes flash) {
        carritoService.vaciar();
        flash.addFlashAttribute("info", "Carrito vaciado.");
        return "redirect:/carrito";
    }
}
