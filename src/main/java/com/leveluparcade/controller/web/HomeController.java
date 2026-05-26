package com.leveluparcade.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador de paginas publicas (home y login placeholder).
 * Verifica que Spring Boot + Thymeleaf + PostgreSQL estan funcionando.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("titulo", "LevelUp Arcade");
        model.addAttribute("mensaje", "Sistema de gestion funcionando correctamente");
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}