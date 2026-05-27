package com.leveluparcade.controller.web;

import com.leveluparcade.dto.request.RegistroClienteRequest;
import com.leveluparcade.service.RecaptchaService;
import com.leveluparcade.service.RegistroClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controlador de autenticacion para clientes (tienda publica).
 *
 * <p>Rutas:
 * <ul>
 *   <li>GET  /login        -> formulario login cliente (Spring Security hace el POST)</li>
 *   <li>GET  /registro     -> formulario de alta</li>
 *   <li>POST /registro     -> procesa el registro y redirige a /login</li>
 * </ul>
 *
 * <p>El panel admin tiene su propio login en /admin/login (otra cadena de Spring Security).
 */
@Controller
public class TiendaAuthController {

    private final RegistroClienteService registroService;
    private final RecaptchaService recaptchaService;
    private final String recaptchaSiteKey;

    public TiendaAuthController(RegistroClienteService registroService,
                                RecaptchaService recaptchaService,
                                @Value("${recaptcha.site-key:}") String recaptchaSiteKey) {
        this.registroService = registroService;
        this.recaptchaService = recaptchaService;
        this.recaptchaSiteKey = recaptchaSiteKey;
    }

    // ---------- LOGIN ----------

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "registrado", required = false) String registrado,
                            Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Email o contrasena incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "Has cerrado sesion correctamente.");
        }
        if (registrado != null) {
            model.addAttribute("registradoMsg",
                    "Cuenta creada con exito. Ya puedes iniciar sesion.");
        }
        return "tienda/login";
    }

    // ---------- REGISTRO ----------

    @GetMapping("/registro")
    public String registroForm(Model model) {
        if (!model.containsAttribute("registro")) {
            model.addAttribute("registro", new RegistroClienteRequest());
        }
        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
        return "tienda/registro";
    }

    @PostMapping("/registro")
    public String registroSubmit(@Valid @ModelAttribute("registro") RegistroClienteRequest req,
                                 BindingResult binding,
                                 RedirectAttributes flash,
                                 Model model) {

        // Validaciones extra antes de tocar BD
        if (!req.isAceptaPrivacidad()) {
            binding.rejectValue("aceptaPrivacidad", "privacidad.required",
                    "Debes aceptar la politica de privacidad");
        }

        if (req.getPassword() != null
                && !req.getPassword().equals(req.getPasswordConfirmacion())) {
            binding.rejectValue("passwordConfirmacion", "password.mismatch",
                    "Las contrasenas no coinciden");
        }

        // reCAPTCHA: solo se valida si esta configurado (modo prod)
        if (!recaptchaService.verificar(req.getRecaptchaToken())) {
            binding.reject("recaptcha.failed",
                    "No hemos podido verificar que no eres un bot. Recarga la pagina e intentalo de nuevo.");
        }

        if (binding.hasErrors()) {
            model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
            return "tienda/registro";
        }

        try {
            registroService.registrar(req);
        } catch (IllegalArgumentException ex) {
            binding.reject("registro.error", ex.getMessage());
            model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
            return "tienda/registro";
        }

        return "redirect:/login?registrado";
    }
}