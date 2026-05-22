package com.leveluparcade.controller.api;

import com.leveluparcade.service.AuthService;
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

        String token = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(new LoginResponse(token));
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