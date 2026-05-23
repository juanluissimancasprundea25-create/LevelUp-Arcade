package com.leveluparcade.dto.response;

import com.leveluparcade.entity.Cliente;

import java.time.LocalDateTime;

/**
 * Representacion de un Cliente para respuestas de la API.
 *
 * <p>Aplana la relacion Cliente ↔ Usuario para devolver una estructura
 * plana al frontend. NUNCA incluye el password hash ni datos sensibles.
 */
public record ClienteResponse(
    Long id,
    String email,
    String nombre,
    String apellidos,
    String nombreCompleto,
    Boolean activo,
    String nif,
    String telefono,
    String direccion,
    String ciudad,
    String codigoPostal,
    String pais,
    LocalDateTime fechaAlta,
    LocalDateTime fechaUltimoLogin
) {

    /** Factoria que construye el response a partir de la entidad. */
    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(
            c.getId(),
            c.getUsuario().getEmail(),
            c.getUsuario().getNombre(),
            c.getUsuario().getApellidos(),
            c.getUsuario().getNombreCompleto(),
            c.getUsuario().getActivo(),
            c.getNif(),
            c.getTelefono(),
            c.getDireccion(),
            c.getCiudad(),
            c.getCodigoPostal(),
            c.getPais(),
            c.getFechaAlta(),
            c.getUsuario().getFechaUltimoLogin()
        );
    }
}