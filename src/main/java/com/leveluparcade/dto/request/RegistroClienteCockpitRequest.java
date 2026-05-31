package com.leveluparcade.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para el registro publico VIA API REST (cockpit SPA).
 *
 * <p>Se llama "Cockpit" para distinguirlo del {@link RegistroClienteRequest}
 * usado por el formulario clasico Thymeleaf, que tiene reglas diferentes
 * (recaptcha, confirmacion de password, aceptacion de privacidad como
 * checkbox de form). En la SPA la confirmacion y privacidad se validan
 * en el cliente, asi que aqui solo viajan los datos finales.
 */
public record RegistroClienteCockpitRequest(

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    @Size(max = 150)
    String email,

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
    String password,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    String nombre,

    @Size(max = 150)
    String apellidos,

    @Pattern(
        regexp = "^[0-9]{8}[A-Za-z]$|^[XYZ][0-9]{7}[A-Za-z]$|^$",
        message = "El NIF debe tener formato valido"
    )
    @Size(max = 20)
    String nif,

    @Pattern(
        regexp = "^[+]?[0-9 ]{6,20}$|^$",
        message = "El telefono no tiene un formato valido"
    )
    @Size(max = 20)
    String telefono,

    @Size(max = 255)
    String direccion,

    @Size(max = 100)
    String ciudad,

    @Pattern(
        regexp = "^[0-9]{4,10}$|^$",
        message = "El codigo postal solo puede contener digitos (4-10)"
    )
    @Size(max = 10)
    String codigoPostal,

    @Size(max = 100)
    String pais
) {}
