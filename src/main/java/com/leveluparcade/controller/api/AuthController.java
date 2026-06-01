package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.ForgotPasswordRequest;
import com.leveluparcade.dto.request.ResetPasswordRequest;
import com.leveluparcade.service.AuthService;
import com.leveluparcade.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthService authService,
                        PasswordResetService passwordResetService,
                        UsuarioRepository usuarioRepository) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

        String token = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(new LoginResponse(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.solicitarRecuperacion(request);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Si el email existe, recibira un enlace de recuperacion en breve."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetearPassword(request);

        return ResponseEntity.ok(Map.of(
            "mensaje", "Contrasena actualizada correctamente. Ya puede iniciar sesion."
        ));
    }

    @GetMapping("/me")
public ResponseEntity<Map<String, Object>> me(Authentication auth) {
    if (auth == null || !auth.isAuthenticated()) {
        return ResponseEntity.status(401).build();
    }

    String email = auth.getName();
    Usuario u = usuarioRepository.findByEmail(email).orElse(null);

    String rolPlano = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("ROLE_NONE")
            .replaceFirst("^ROLE_", "");

    Map<String, Object> body = new java.util.HashMap<>();
    body.put("email",  email);
    body.put("rol",    rolPlano);
    body.put("nombre", u != null ? u.getNombre() : email.split("@")[0]);
    body.put("id",     u != null ? u.getId()     : null);

    return ResponseEntity.ok(body);
}

    // DTO LOGIN REQUEST
    @Data
    public static class LoginRequest {
        private String email;
        private String password;
    }

    // DTO LOGIN RESPONSE (SOLO TOKEN)
    @Data
    @NoArgsConstructor
    public static class LoginResponse {
        private String token;

        public LoginResponse(String token) {
            this.token = token;
        }
    }
}