package com.leveluparcade.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controlador de paginas publicas y dashboard.
 *  /          -> landing publica
 *  /login     -> form de login (GET muestra, POST lo gestiona Spring Security)
 *  /dashboard -> vista principal del panel, solo ROLE_ADMIN
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@RequestParam(required = false) String logout, Model model) {
        model.addAttribute("titulo", "LevelUp Arcade");
        model.addAttribute("mensaje", "Sistema de gestion funcionando correctamente");
        if (logout != null) {
            model.addAttribute("logoutOk", true);
        }
        return "home";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Email o contrasena incorrectos");
        }
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("usuarioActual", auth.getName());
        model.addAttribute("titulo", "Dashboard");
        model.addAttribute("tituloSeccion", "Dashboard");
        return "dashboard";
    }
}