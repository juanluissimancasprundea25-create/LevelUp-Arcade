package com.leveluparcade.controller.web;

import com.leveluparcade.service.CarritoService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Inyecta el numero de unidades del carrito en el modelo de TODAS las
 * vistas, para pintar el badge del navbar de la tienda.
 *
 * <p>Si no hay cliente logueado, {@link CarritoService#contarUnidades()}
 * devuelve 0 sin lanzar error, asi que el badge simplemente no aparece.
 *
 * <p>Se limita a controllers del paquete web.* (anotacion basePackages),
 * que son los que renderizan vistas Thymeleaf. No afecta a la API REST.
 */
@ControllerAdvice(basePackages = "com.leveluparcade.controller.web")
public class CarritoModelAdvice {

    private final CarritoService carritoService;

    public CarritoModelAdvice(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @ModelAttribute("carritoUnidades")
    public int carritoUnidades() {
        try {
            return carritoService.contarUnidades();
        } catch (Exception ex) {
            // Defensivo: nunca romper el render por el badge.
            return 0;
        }
    }
}
