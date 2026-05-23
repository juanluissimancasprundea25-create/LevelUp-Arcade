package com.leveluparcade.security;

import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper para acceder al usuario autenticado desde cualquier capa.
 *
 * <p>Existe para no repetir la logica de "saca el email del
 * SecurityContext y busca el usuario en BD" en multiples sitios.
 */
@Component
public class SecurityHelper {

    private final UsuarioRepository usuarioRepository;

    public SecurityHelper(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Devuelve el id del usuario autenticado, o null si no hay sesion
     * o el principal no se corresponde con un usuario de la BD.
     */
    public Long getUsuarioActualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return usuarioRepository.findByEmail(auth.getName())
                .map(Usuario::getId)
                .orElse(null);
    }
}