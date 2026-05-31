package com.leveluparcade.controller.api;

import com.leveluparcade.dto.response.UsuarioLookupResponse;
import com.leveluparcade.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints minimos de usuarios para uso interno del panel admin.
 *
 * <p>Por ahora solo expone un "lookup" plano que devuelve {id, email,
 * nombre, rol, activo} para todos los usuarios del sistema. Lo usa el
 * modulo Auditoria para resolver IDs a nombres y para el autocomplete
 * del filtro por usuario.
 *
 * <p>Restringido a ADMIN. No expone passwords ni datos sensibles.
 *
 * <p>Si en el futuro el sistema crece a miles de usuarios, sustituir
 * por una version paginada con busqueda server-side.
 */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioLookupApiController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioLookupApiController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Lista plana de todos los usuarios. Solo datos publicos. */
    @GetMapping("/lookup")
    public List<UsuarioLookupResponse> lookup() {
        return usuarioRepository.findAll().stream()
            .map(UsuarioLookupResponse::from)
            .toList();
    }
}
