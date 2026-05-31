package com.leveluparcade.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards de rutas SPA a /app/index.html.
 *
 * <p>El frontend React vive en /app/ (compilado por Vite en
 * src/main/resources/static/app/). React Router gestiona el
 * enrutado client-side, así que cualquier petición a /cockpit
 * o /cockpit/lo-que-sea tiene que devolver el mismo index.html
 * y dejar que JS haga su trabajo.
 *
 * <p>El HomeController Thymeleaf existente sigue intacto: "/"
 * sigue sirviendo la home antigua. La SPA se prueba en /cockpit.
 */
@Controller
public class SpaController {

    /** /cockpit exacto */
    @GetMapping("/cockpit")
    public String cockpitRoot() {
        return "forward:/app/index.html";
    }

    /** /cockpit/cualquier-cosa (rutas de React Router) */
    @GetMapping("/cockpit/**")
    public String cockpitSubpaths() {
        return "forward:/app/index.html";
    }
}