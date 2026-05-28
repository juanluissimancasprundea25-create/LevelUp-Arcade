package com.leveluparcade.controller.web;

import com.leveluparcade.service.CarritoService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Inyecta el numero de unidades del carrito en el modelo de TODAS las
 * vistas de la tienda, para pintar el badge del navbar.
 *
 * <p>Usa {@link ObjectProvider} para que el bean sea opcional en el
 * contexto de {@code @WebMvcTest} (slices que no cargan CarritoService).
 * En runtime real el bean siempre existe.
 */
@ControllerAdvice(basePackages = "com.leveluparcade.controller.web")
public class CarritoModelAdvice {

    private final ObjectProvider<CarritoService> carritoServiceProvider;

    public CarritoModelAdvice(ObjectProvider<CarritoService> carritoServiceProvider) {
        this.carritoServiceProvider = carritoServiceProvider;
    }

    @ModelAttribute("carritoUnidades")
    public int carritoUnidades() {
        CarritoService carritoService = carritoServiceProvider.getIfAvailable();
        if (carritoService == null) {
            return 0;
        }
        try {
            return carritoService.contarUnidades();
        } catch (Exception ex) {
            // Defensivo: nunca romper el render por el badge.
            return 0;
        }
    }
}
