package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Peticion para confirmar el cambio de contrasena con un token valido.
 */
public record ResetPasswordRequest(

    @NotBlank(message = "El token es obligatorio")
    @Size(max = 255)
    String token,

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "La contrasena debe contener al menos una letra y un numero"
    )
    String nuevaPassword

) {}