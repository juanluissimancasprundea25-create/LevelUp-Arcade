package com.leveluparcade.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Peticion para iniciar el flujo de recuperacion de contrasena.
 *
 * <p>Por seguridad, el endpoint siempre devuelve respuesta exitosa
 * aunque el email no exista (evita enumeracion de cuentas).
 */
public record ForgotPasswordRequest(

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato valido")
    @Size(max = 150)
    String email

) {}