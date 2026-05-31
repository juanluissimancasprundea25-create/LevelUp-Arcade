package com.leveluparcade.dto.response;

import com.leveluparcade.entity.Usuario;

/**
 * Representacion minima de un usuario para listas de lookup
 * (ej. resolver IDs a nombres en el log de auditoria).
 *
 * <p>NO incluye password hash ni datos sensibles — solo lo necesario
 * para identificar visualmente al usuario en la UI.
 */
public record UsuarioLookupResponse(
    Long id,
    String email,
    String nombre,
    String apellidos,
    String nombreCompleto,
    String rol,
    Boolean activo
) {
    public static UsuarioLookupResponse from(Usuario u) {
        return new UsuarioLookupResponse(
            u.getId(),
            u.getEmail(),
            u.getNombre(),
            u.getApellidos(),
            u.getNombreCompleto(),
            u.getRol() != null ? u.getRol().name() : null,
            u.getActivo()
        );
    }
}
