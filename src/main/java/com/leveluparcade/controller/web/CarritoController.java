package com.leveluparcade.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Stub temporal del carrito. La funcionalidad real entra en el PR #23.
 *
 * <p>Existe ahora solo para que el boton "Anadir al carrito" del detalle
 * de producto del PR #22 no produzca un 404. Se limita a devolver un
 * mensaje flash y mandar al usuario de vuelta al detalle.
 *
 * <p>NO se trata de funcionalidad real: no toca sesion, no toca BD.
 */
@Controller
public class CarritoController {

    @PostMapping("/carrito/add")
    public String addStub(@RequestParam("productoId") Long productoId,
                          @RequestParam(value = "cantidad", required = false, defaultValue = "1") Integer cantidad,
                          RedirectAttributes flash) {
        flash.addFlashAttribute("info",
                "El carrito estara disponible en la proxima version. Gracias por tu paciencia.");
        return "redirect:/producto/" + productoId;
    }
}
