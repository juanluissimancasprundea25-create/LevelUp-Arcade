package com.leveluparcade.service.impl;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.dto.request.ForgotPasswordRequest;
import com.leveluparcade.dto.request.ResetPasswordRequest;
import com.leveluparcade.entity.PasswordResetToken;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.PasswordResetTokenRepository;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.PasswordResetProperties;
import com.leveluparcade.service.EmailService;
import com.leveluparcade.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementacion del flujo de recuperacion de contrasena.
 *
 * <p>Politica de seguridad:
 * <ul>
 *   <li>El endpoint forgot no revela si el email existe.</li>
 *   <li>Solicitar un nuevo token invalida los anteriores del mismo usuario.</li>
 *   <li>El token se marca como usado tras un reset exitoso (single-use).</li>
 *   <li>El cambio de contrasena se audita.</li>
 * </ul>
 */
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordResetProperties props;
    private final AuditoriaPublisher auditoria;

    public PasswordResetServiceImpl(UsuarioRepository usuarioRepository,
                                    PasswordResetTokenRepository tokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    EmailService emailService,
                                    PasswordResetProperties props,
                                    AuditoriaPublisher auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.props = props;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public void solicitarRecuperacion(ForgotPasswordRequest request) {

        Optional<Usuario> opt = usuarioRepository.findByEmail(request.email());
        if (opt.isEmpty()) {
            log.info("Solicitud de reset para email inexistente: {}", request.email());
            return;
        }

        Usuario usuario = opt.get();

        if (Boolean.FALSE.equals(usuario.getActivo())) {
            log.info("Solicitud de reset ignorada para usuario inactivo id={}", usuario.getId());
            return;
        }

        tokenRepository.invalidarTokensActivos(usuario);

        int horas = props.tokenExpirationHours() == null ? 1 : props.tokenExpirationHours();
        PasswordResetToken token = PasswordResetToken.builder()
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .fechaExpiracion(LocalDateTime.now().plusHours(horas))
                .usado(false)
                .build();

        token = tokenRepository.save(token);

        String enlace = props.baseUrl() + "/reset-password?token=" + token.getToken();
        emailService.enviarEmailRecuperacionPassword(
            usuario.getEmail(), usuario.getNombre(), enlace);

        log.info("Token de reset generado para usuario id={}", usuario.getId());
    }

    @Override
    @Transactional
    public void resetearPassword(ResetPasswordRequest request) {

        PasswordResetToken token = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Token invalido o ya utilizado"));

        if (!token.esValido()) {
            throw new IllegalArgumentException(
                "El token ha caducado o ya fue utilizado. Solicite uno nuevo.");
        }

        Usuario usuario = token.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.nuevaPassword()));
        usuarioRepository.save(usuario);

        token.setUsado(true);
        tokenRepository.save(token);

        log.info("Password reseteada para usuario id={}", usuario.getId());

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.LOGIN_EXITO, "Usuario",
            usuario.getId(), usuario.getId(),
            "Password reseteada via token de recuperacion"));
    }
}