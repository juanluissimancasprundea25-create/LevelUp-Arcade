package com.leveluparcade.security;

import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.ClienteRepository;
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
    private final ClienteRepository clienteRepository;

    public SecurityHelper(UsuarioRepository usuarioRepository,
                          ClienteRepository clienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
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

    /**
     * Devuelve el id del Cliente asociado al usuario autenticado, o null
     * si no hay sesion o el usuario no tiene un Cliente asociado
     * (por ejemplo, si es un ADMIN o EMPLEADO sin entidad Cliente).
     *
     * <p>Util en endpoints "mios" o para verificar propiedad de recursos.
     */
    public Long getClienteActualId() {
        Long usuarioId = getUsuarioActualId();
        if (usuarioId == null) {
            return null;
        }
        return clienteRepository.findByUsuarioId(usuarioId)
                .map(Cliente::getId)
                .orElse(null);
    }
}