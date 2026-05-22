package com.leveluparcade.service;

import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * LOGIN + JWT
     */
    public String login(String email, String password) {

        Usuario user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        return jwtService.generateToken(user.getEmail());
    }
}