package com.leveluparcade.controller.web;

import com.leveluparcade.dto.response.FacturaResponse;
import com.leveluparcade.service.FacturaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controlador PUBLICO (sin login) para la verificacion de facturas.
 *
 * <p>El QR que se incluye en el PDF de las facturas apunta a esta ruta.
 * Cualquier persona con el numero de factura puede comprobar su validez.
 *
 * <p>El resto de operaciones sobre facturas (listar, descargar, emitir)
 * estan en {@link FacturaWebController} bajo /admin/facturas.
 */
@Controller
@RequestMapping("/facturas")
public class FacturaPublicaController {

    private final FacturaService facturaService;

    public FacturaPublicaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping("/verificar/{numeroFactura}")
    public String verificar(@PathVariable String numeroFactura, Model model) {
        try {
            FacturaResponse factura = facturaService.verificar(numeroFactura);
            model.addAttribute("factura", factura);
            model.addAttribute("valida", true);
        } catch (Exception ex) {
            model.addAttribute("valida", false);
            model.addAttribute("numeroFactura", numeroFactura);
            model.addAttribute("error", ex.getMessage());
        }
        return "facturas/verificar";
    }
}