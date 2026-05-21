package com.leveluparcade.controller.api;

import com.leveluparcade.entity.Usuario;
import com.leveluparcade.service.AuthService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Usuario user = authService.login(request.getEmail(), request.getPassword());
        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getNombre(),
                user.getApellidos(),
                user.getRol().name()
        );
        return ResponseEntity.ok(response);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private Long id;
        private String email;
        private String nombre;
        private String apellidos;
        private String rol;
    }
}