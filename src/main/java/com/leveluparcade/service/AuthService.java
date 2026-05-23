package com.leveluparcade.service;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaPublisher auditoriaPublisher;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditoriaPublisher auditoriaPublisher) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoriaPublisher = auditoriaPublisher;
    }

    /**
     * LOGIN + JWT.
     *
     * <p>Publica eventos de auditoria tanto en exito como en fallo,
     * para registrar en auditoria_log cualquier intento de acceso.
     */
    public String login(String email, String password) {

        Optional<Usuario> optUser = usuarioRepository.findByEmail(email);

        if (optUser.isEmpty()) {
            auditoriaPublisher.publish(AuditoriaEvent.autenticacion(
                TipoEvento.LOGIN_FALLIDO, null,
                "Intento de login con email inexistente: " + email));
            throw new RuntimeException("Credenciales invalidas");
        }

        Usuario user = optUser.get();

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            auditoriaPublisher.publish(AuditoriaEvent.autenticacion(
                TipoEvento.LOGIN_FALLIDO, user.getId(),
                "Password incorrecta para: " + email));
            throw new RuntimeException("Credenciales invalidas");
        }

        auditoriaPublisher.publish(AuditoriaEvent.autenticacion(
            TipoEvento.LOGIN_EXITO, user.getId(),
            "Login exitoso: " + email));

        return jwtService.generateToken(user.getEmail());
    }
}