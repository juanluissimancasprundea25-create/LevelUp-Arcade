package com.leveluparcade.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que un cliente puede actualizar de su propio perfil.
 *
 * <p>NO incluye email ni password (esos se gestionan por separado por
 * razones de seguridad). El NIF solo se puede establecer si esta vacio
 * — una vez fijado no se cambia desde aqui para preservar la integridad
 * fiscal.
 */
public record ActualizarMiPerfilRequest(

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String nombre,

    @Size(max = 150)
    String apellidos,

    @Pattern(
        regexp = "^[0-9]{8}[A-Za-z]$|^[XYZ][0-9]{7}[A-Za-z]$|^$",
        message = "NIF invalido"
    )
    @Size(max = 20)
    String nif,

    @Pattern(
        regexp = "^[+]?[0-9 ]{6,20}$|^$",
        message = "Telefono invalido"
    )
    @Size(max = 20)
    String telefono,

    @Size(max = 255)
    String direccion,

    @Size(max = 100)
    String ciudad,

    @Pattern(
        regexp = "^[0-9]{4,10}$|^$",
        message = "Codigo postal: solo digitos (4-10)"
    )
    @Size(max = 10)
    String codigoPostal,

    @Size(max = 100)
    String pais
) {}
