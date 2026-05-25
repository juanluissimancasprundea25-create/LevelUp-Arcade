package com.leveluparcade.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuracion del flujo de recuperacion de contrasena.
 *
 * <p>Se cargan de {@code application.yml} bajo el prefijo {@code password-reset}.
 */
@ConfigurationProperties(prefix = "password-reset")
public record PasswordResetProperties(
    Integer tokenExpirationHours,
    String baseUrl,
    String fromEmail,
    String fromName
) {}