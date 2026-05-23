package com.leveluparcade.dto.response;

import com.leveluparcade.entity.Proveedor;

import java.time.LocalDateTime;

/** Representacion de un Proveedor en respuestas de la API. */
public record ProveedorResponse(
    Long id,
    String nombreEmpresa,
    String cif,
    String emailContacto,
    String telefono,
    String direccion,
    String ciudad,
    String codigoPostal,
    String pais,
    Boolean activo,
    LocalDateTime fechaAlta
) {

    public static ProveedorResponse from(Proveedor p) {
        return new ProveedorResponse(
            p.getId(),
            p.getNombreEmpresa(),
            p.getCif(),
            p.getEmailContacto(),
            p.getTelefono(),
            p.getDireccion(),
            p.getCiudad(),
            p.getCodigoPostal(),
            p.getPais(),
            p.getActivo(),
            p.getFechaAlta()
        );
    }
}