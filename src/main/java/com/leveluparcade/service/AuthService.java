package com.leveluparcade.service;

import com.leveluparcade.entity.PasswordResetToken;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.PasswordResetTokenRepository;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    /**
     * LOGIN + JWT
     */
    public String login(String email, String password) {

        Usuario user = usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Credenciales inválidas"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        return jwtService.generateToken(user.getEmail());
    }

    /**
     * ENVIAR EMAIL DE RECUPERACIÓN
     */
    public void sendPasswordResetToken(String email) {

        usuarioRepository.findByEmail(email)
                .ifPresent(usuario -> {

                    // eliminar tokens antiguos
                    tokenRepository.deleteByUsuario(usuario);

                    // crear token
                    String token = UUID.randomUUID().toString();

                    PasswordResetToken resetToken =
                            PasswordResetToken.builder()
                                    .token(token)
                                    .usuario(usuario)
                                    .expirationDate(
                                            LocalDateTime.now().plusMinutes(30))
                                    .build();

                    tokenRepository.save(resetToken);

                    String link =
                            "http://localhost:8080/reset-password?token=" + token;

                    emailService.sendEmail(
                            usuario.getEmail(),
                            "Recuperación de contraseña",
                            """
                            Has solicitado recuperar tu contraseña.

                            Pulsa el enlace:

                            %s

                            Este enlace caduca en 30 minutos.
                            """.formatted(link)
                    );
                });
    }

    /**
     * RESET PASSWORD
     */
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken =
                tokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException("Token inválido"));

        if (resetToken.getExpirationDate()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException("Token expirado");
        }

        Usuario usuario = resetToken.getUsuario();

        usuario.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );

        usuarioRepository.save(usuario);

        tokenRepository.delete(resetToken);
    }
}