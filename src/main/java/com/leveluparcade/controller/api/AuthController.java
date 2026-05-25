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

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
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