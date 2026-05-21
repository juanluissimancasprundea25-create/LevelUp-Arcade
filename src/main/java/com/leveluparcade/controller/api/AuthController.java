package com.leveluparcade.controller.api;

import com.leveluparcade.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> body) {

        String token = authService.login(
                body.get("email"),
                body.get("password")
        );

        return Map.of("token", token);
    }
}