package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para actualizar un Cliente existente.
 *
 * <p>NO se permite cambiar el email aqui (porque es el identificador
 * de login y cambiarlo requiere flujo de verificacion separado).
 *
 * <p>NO se permite cambiar la password aqui (existe flujo dedicado).
 */
public record ClienteUpdateRequest(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String nombre,

    @Size(max = 150, message = "Los apellidos no pueden superar 150 caracteres")
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
    String pais,

    /** Permite al admin activar/desactivar al cliente. */
    Boolean activo
) {}