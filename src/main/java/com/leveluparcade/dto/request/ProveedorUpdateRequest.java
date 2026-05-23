package com.leveluparcade.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para actualizar un proveedor existente.
 *
 * <p>Permite actualizar todos los campos incluyendo el CIF (con
 * validacion de unicidad en el service). Tambien permite activar/
 * desactivar al proveedor.
 */
public record ProveedorUpdateRequest(

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 200)
    String nombreEmpresa,

    @NotBlank(message = "El CIF es obligatorio")
    @Pattern(
        regexp = "^[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]$",
        message = "El CIF debe tener formato espanol valido (ej: A12345678)"
    )
    @Size(max = 20)
    String cif,

    @Email(message = "El email no tiene un formato valido")
    @Size(max = 150)
    String emailContacto,

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

    Boolean activo
) {}