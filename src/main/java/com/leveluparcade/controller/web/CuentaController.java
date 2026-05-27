package com.leveluparcade.controller.web;

import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.UsuarioRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Area privada del cliente.
 *
 * <p>Por ahora solo muestra una bienvenida con el nombre del usuario.
 * En PRs posteriores se anaden subseccciones: pedidos, facturas, devoluciones,
 * perfil, chat con admin.
 */
@Controller
@RequestMapping("/cuenta")
public class CuentaController {

    private final UsuarioRepository usuarioRepository;

    public CuentaController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String inicio(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();
        model.addAttribute("usuario", usuario);
        model.addAttribute("seccionCuenta", "inicio");
        return "tienda/cuenta/inicio";
    }
}