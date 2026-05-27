package com.leveluparcade.service;

import com.leveluparcade.dto.request.RegistroClienteRequest;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.Rol;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que orquesta el registro publico de un cliente.
 *
 * <p>El flujo:
 * <ol>
 *   <li>Valida que las contrasenas coinciden</li>
 *   <li>Valida que el email no esta ya en uso</li>
 *   <li>Crea un Usuario con rol CLIENTE y password hasheada</li>
 *   <li>Crea un Cliente asociado al usuario</li>
 * </ol>
 *
 * <p>NO valida el reCAPTCHA: eso lo hace el controlador antes de llamar aqui.
 */
@Service
public class RegistroClienteService {

    private static final Logger log = LoggerFactory.getLogger(RegistroClienteService.class);

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroClienteService(UsuarioRepository usuarioRepository,
                                  ClienteRepository clienteRepository,
                                  PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un nuevo cliente.
     *
     * @return el cliente creado
     * @throws IllegalArgumentException si las contrasenas no coinciden,
     *         el email ya existe o no se acepto la politica de privacidad
     */
    @Transactional
    public Cliente registrar(RegistroClienteRequest req) {
        if (!req.isAceptaPrivacidad()) {
            throw new IllegalArgumentException("Debes aceptar la politica de privacidad");
        }

        if (req.getPassword() == null || !req.getPassword().equals(req.getPasswordConfirmacion())) {
            throw new IllegalArgumentException("Las contrasenas no coinciden");
        }

        String emailNormalizado = req.getEmail().trim().toLowerCase();

        if (usuarioRepository.findByEmail(emailNormalizado).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = Usuario.builder()
                .email(emailNormalizado)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .nombre(req.getNombre().trim())
                .apellidos(req.getApellidos() != null ? req.getApellidos().trim() : null)
                .rol(Rol.CLIENTE)
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);

        Cliente cliente = Cliente.builder()
                .usuario(usuario)
                .build();

        cliente = clienteRepository.save(cliente);

        log.info("Cliente registrado correctamente: id={} email={}", cliente.getId(), usuario.getEmail());
        return cliente;
    }
}