package com.leveluparcade.service.impl;

import com.leveluparcade.security.PasswordResetProperties;
import com.leveluparcade.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementacion de {@link EmailService} usando Spring Mail.
 *
 * <p>Si {@code spring.mail.username} esta vacio, el envio se simula
 * (se loguea el contenido) en vez de fallar. Esto permite trabajar en
 * desarrollo sin configurar Gmail.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;
    private final PasswordResetProperties props;
    private final String mailUsername;

    public EmailServiceImpl(JavaMailSender mailSender,
                            PasswordResetProperties props,
                            @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.props = props;
        this.mailUsername = mailUsername;
    }

    @Override
    public void enviarEmailRecuperacionPassword(String destinatario,
                                                String nombreUsuario,
                                                String enlaceReset) {

        String asunto = "Recuperacion de contrasena - LevelUp Arcade";
        String cuerpo = construirCuerpo(nombreUsuario, enlaceReset);

        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("======================================================");
            log.warn("SMTP no configurado. Simulando envio de email:");
            log.warn("PARA: {}", destinatario);
            log.warn("ASUNTO: {}", asunto);
            log.warn("CUERPO:\n{}", cuerpo);
            log.warn("======================================================");
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(props.fromEmail());
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);

            mailSender.send(mensaje);
            log.info("Email de recuperacion enviado a {}", destinatario);

        } catch (Exception e) {
            log.error("Fallo al enviar email a {}: {}", destinatario, e.getMessage());
            // No relanzamos: el endpoint no debe revelar si el envio fallo
        }
    }

    private String construirCuerpo(String nombre, String enlace) {
        int horas = props.tokenExpirationHours() == null ? 1 : props.tokenExpirationHours();
        return String.format("""
            Hola %s,

            Has solicitado restablecer tu contrasena en LevelUp Arcade.

            Para continuar, pulsa el siguiente enlace (caduca en %d hora(s)):

            %s

            Si no has sido tu, ignora este mensaje y tu contrasena seguira igual.

            -- 
            %s
            """,
            nombre == null ? "" : nombre,
            horas,
            enlace,
            props.fromName() == null ? "LevelUp Arcade" : props.fromName()
        );
    }
}