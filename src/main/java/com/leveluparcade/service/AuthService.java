package com.leveluparcade.service;

import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository repo,
                       PasswordEncoder encoder,
                       JwtService jwtService) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public String login(String email, String password) {

        Usuario user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Password incorrecta");
        }

        return jwtService.generateToken(user.getEmail());
    }
}