package com.leveluparcade.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para dar de alta un Cliente desde el panel admin.
 *
 * <p>El alta crea internamente: (1) un Usuario con rol CLIENTE y una
 * password temporal auto-generada, y (2) el registro Cliente asociado.
 * La password temporal se devuelve en {@code PasswordTemporalResponse}
 * y solo se muestra al admin UNA vez.
 *
 * <p>Los campos comerciales (nif, telefono, direccion...) son opcionales:
 * el admin puede crear un cliente solo con sus datos basicos y dejar
 * que el propio cliente complete su perfil despues.
 */
public record ClienteCreateRequest(

    // --- Datos del Usuario asociado ---

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    String email,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String nombre,

    @Size(max = 150, message = "Los apellidos no pueden superar 150 caracteres")
    String apellidos,

    // --- Datos comerciales (todos opcionales) ---

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

    @Size(max = 255, message = "La direccion no puede superar 255 caracteres")
    String direccion,

    @Size(max = 100, message = "La ciudad no puede superar 100 caracteres")
    String ciudad,

    @Pattern(
        regexp = "^[0-9]{4,10}$|^$",
        message = "El codigo postal solo puede contener digitos (4-10)"
    )
    @Size(max = 10)
    String codigoPostal,

    @Size(max = 100, message = "El pais no puede superar 100 caracteres")
    String pais
) {}