package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.ActualizarMiPerfilRequest;
import com.leveluparcade.dto.request.CambiarPasswordRequest;
import com.leveluparcade.dto.response.ClienteResponse;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.SecurityHelper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints "/me" para que el cliente gestione sus propios datos sin
 * necesidad de permisos de admin.
 *
 * <ul>
 *   <li>GET  /api/mi-perfil — ver mi perfil</li>
 *   <li>PUT  /api/mi-perfil — editar mis datos (sin email ni password)</li>
 *   <li>POST /api/mi-perfil/cambiar-password — cambiar contrasena</li>
 * </ul>
 *
 * <p>Solo accesible para CLIENTE. Los admins gestionan sus propios datos
 * por otros medios (no son "clientes" en el modelo).
 */
@RestController
@RequestMapping("/api/mi-perfil")
@PreAuthorize("hasRole('CLIENTE')")
public class MiPerfilApiController {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityHelper securityHelper;

    public MiPerfilApiController(ClienteRepository clienteRepository,
                                   UsuarioRepository usuarioRepository,
                                   PasswordEncoder passwordEncoder,
                                   SecurityHelper securityHelper) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityHelper = securityHelper;
    }

    /** Mis datos personales. */
    @GetMapping
    @Transactional(readOnly = true)
    public ClienteResponse miPerfil() {
        return ClienteResponse.from(obtenerMiCliente());
    }

    /** Actualiza mis datos. No permite cambiar email ni password. */
    @PutMapping
    @Transactional
    public ClienteResponse actualizar(@Valid @RequestBody ActualizarMiPerfilRequest req) {
        Cliente cliente = obtenerMiCliente();
        Usuario usuario = cliente.getUsuario();

        // Datos del usuario
        if (req.nombre() != null && !req.nombre().isBlank()) {
            usuario.setNombre(req.nombre().trim());
        }
        if (req.apellidos() != null) {
            // Permitir vaciar apellidos
            usuario.setApellidos(req.apellidos().isBlank() ? null : req.apellidos().trim());
        }

        // Datos del cliente
        if (req.nif() != null && !req.nif().isBlank()) {
            String nifNuevo = req.nif().trim().toUpperCase();
            // Si el cliente ya tiene NIF, no permitir cambiarlo (integridad fiscal).
            // Solo permitir establecerlo si estaba vacio.
            if (cliente.getNif() == null || cliente.getNif().isBlank()) {
                // Verificar que no este en uso por otro cliente
                clienteRepository.findByNif(nifNuevo).ifPresent(otro -> {
                    if (!otro.getId().equals(cliente.getId())) {
                        throw new IllegalArgumentException("Ese NIF ya esta en uso");
                    }
                });
                cliente.setNif(nifNuevo);
            }
            // Si ya tenia NIF y el enviado coincide, no hacemos nada.
            // Si ya tenia NIF y mandan otro distinto, lo ignoramos silenciosamente.
        }

        // Resto de campos: permitir vaciar enviando string vacio
        cliente.setTelefono(vacioANull(req.telefono()));
        cliente.setDireccion(vacioANull(req.direccion()));
        cliente.setCiudad(vacioANull(req.ciudad()));
        cliente.setCodigoPostal(vacioANull(req.codigoPostal()));
        cliente.setPais(vacioANull(req.pais()));

        usuarioRepository.save(usuario);
        Cliente actualizado = clienteRepository.save(cliente);
        return ClienteResponse.from(actualizado);
    }

    /** Cambia la contrasena verificando la actual. */
    @PostMapping("/cambiar-password")
    @Transactional
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest req) {
        Cliente cliente = obtenerMiCliente();
        Usuario usuario = cliente.getUsuario();

        // Verificar password actual
        if (!passwordEncoder.matches(req.passwordActual(), usuario.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                Map.of("mensaje", "La contrasena actual no es correcta")
            );
        }

        // Que la nueva sea distinta
        if (passwordEncoder.matches(req.passwordNueva(), usuario.getPasswordHash())) {
            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "La nueva contrasena debe ser distinta de la actual")
            );
        }

        usuario.setPasswordHash(passwordEncoder.encode(req.passwordNueva()));
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Contrasena actualizada correctamente"));
    }

    /* ---------- helpers ---------- */

    private Cliente obtenerMiCliente() {
        Long usuarioId = securityHelper.getUsuarioActualId();
        if (usuarioId == null) {
            throw new AccessDeniedException("No autenticado");
        }
        return clienteRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No se encontro el perfil del cliente actual"));
    }

    private static String vacioANull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
