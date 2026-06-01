package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de contrasena del usuario autenticado.
 *
 * <p>Requiere la actual para evitar que un atacante que haya secuestrado
 * el token JWT pueda cambiar la contrasena directamente.
 */
public record CambiarPasswordRequest(

    @NotBlank(message = "La contrasena actual es obligatoria")
    String passwordActual,

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, max = 100, message = "La nueva contrasena debe tener entre 8 y 100 caracteres")
    String passwordNueva
) {}
