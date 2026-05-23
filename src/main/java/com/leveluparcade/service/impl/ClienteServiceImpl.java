package com.leveluparcade.service.impl;

import com.leveluparcade.dto.request.ClienteCreateRequest;
import com.leveluparcade.dto.request.ClienteUpdateRequest;
import com.leveluparcade.dto.response.ClienteCreadoResponse;
import com.leveluparcade.dto.response.ClienteResponse;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.Rol;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.service.ClienteService;
import com.leveluparcade.util.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion de {@link ClienteService}.
 *
 * <p>Toda la logica de creacion/actualizacion es transaccional para
 * garantizar consistencia entre Usuario y Cliente (que son entidades
 * separadas con relacion 1:1).
 */
@Service
public class ClienteServiceImpl implements ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteServiceImpl.class);

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              UsuarioRepository usuarioRepository,
                              PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(ClienteResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }
        return clienteRepository.buscarPorTexto(texto.trim()).stream()
                .map(ClienteResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse obtenerPorId(Long id) {
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        return ClienteResponse.from(c);
    }

    @Override
    @Transactional
    public ClienteCreadoResponse crear(ClienteCreateRequest req) {

        // 1. Validar que no exista email ni NIF duplicados
        if (usuarioRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException(
                "Ya existe un usuario con el email: " + req.email());
        }
        if (req.nif() != null && !req.nif().isBlank()
                && clienteRepository.existsByNif(req.nif())) {
            throw new IllegalArgumentException(
                "Ya existe un cliente con el NIF: " + req.nif());
        }

        // 2. Generar password temporal y hashearla
        String passwordTemporal = PasswordGenerator.generar();
        String passwordHash = passwordEncoder.encode(passwordTemporal);

        // 3. Construir Usuario (rol CLIENTE, activo)
        Usuario usuario = Usuario.builder()
                .email(req.email())
                .passwordHash(passwordHash)
                .nombre(req.nombre())
                .apellidos(req.apellidos())
                .rol(Rol.CLIENTE)
                .activo(true)
                .build();

        // 4. Construir Cliente con el Usuario embebido
        //    La cascada PERSIST de Cliente -> Usuario hara el insert de ambos
        Cliente cliente = Cliente.builder()
                .usuario(usuario)
                .nif(nullSiVacio(req.nif()))
                .telefono(nullSiVacio(req.telefono()))
                .direccion(nullSiVacio(req.direccion()))
                .ciudad(nullSiVacio(req.ciudad()))
                .codigoPostal(nullSiVacio(req.codigoPostal()))
                .pais(req.pais() == null || req.pais().isBlank() ? "Espana" : req.pais())
                .build();

        Cliente guardado = clienteRepository.save(cliente);

        log.info("Cliente creado: id={}, email={}", guardado.getId(), req.email());

        // 5. Devolver el cliente + password temporal en plano (UNICA vez)
        return new ClienteCreadoResponse(
            ClienteResponse.from(guardado),
            passwordTemporal
        );
    }

    @Override
    @Transactional
    public ClienteResponse actualizar(Long id, ClienteUpdateRequest req) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));

        // Validar NIF unico si cambia
        if (req.nif() != null && !req.nif().isBlank()
                && !req.nif().equals(cliente.getNif())
                && clienteRepository.existsByNif(req.nif())) {
            throw new IllegalArgumentException(
                "Ya existe otro cliente con el NIF: " + req.nif());
        }

        // Actualizar Usuario (nombre, apellidos, activo)
        Usuario u = cliente.getUsuario();
        u.setNombre(req.nombre());
        u.setApellidos(req.apellidos());
        if (req.activo() != null) {
            u.setActivo(req.activo());
        }

        // Actualizar Cliente (datos comerciales)
        cliente.setNif(nullSiVacio(req.nif()));
        cliente.setTelefono(nullSiVacio(req.telefono()));
        cliente.setDireccion(nullSiVacio(req.direccion()));
        cliente.setCiudad(nullSiVacio(req.ciudad()));
        cliente.setCodigoPostal(nullSiVacio(req.codigoPostal()));
        if (req.pais() != null && !req.pais().isBlank()) {
            cliente.setPais(req.pais());
        }

        Cliente actualizado = clienteRepository.save(cliente);
        log.info("Cliente actualizado: id={}", id);

        return ClienteResponse.from(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));
        clienteRepository.delete(cliente);
        log.info("Cliente eliminado: id={}", id);
    }

    // ---------- helpers ----------

    /** Normaliza strings vacios a null para no guardar "" en BD. */
    private String nullSiVacio(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}