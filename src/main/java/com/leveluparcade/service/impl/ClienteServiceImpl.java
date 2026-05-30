package com.leveluparcade.service.impl;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.dto.request.ClienteCreateRequest;
import com.leveluparcade.dto.request.ClienteUpdateRequest;
import com.leveluparcade.dto.response.ClienteCreadoResponse;
import com.leveluparcade.dto.response.ClienteResponse;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.Rol;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.repository.PedidoRepository;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.SecurityHelper;
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
 *
 * <p>Cada operacion de escritura publica un evento de auditoria que
 * persiste {@code AuditoriaListener}.
 */
@Service
public class ClienteServiceImpl implements ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteServiceImpl.class);

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaPublisher auditoria;
    private final SecurityHelper securityHelper;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              UsuarioRepository usuarioRepository,
                              PedidoRepository pedidoRepository,
                              PasswordEncoder passwordEncoder,
                              AuditoriaPublisher auditoria,
                              SecurityHelper securityHelper) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
        this.securityHelper = securityHelper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        // Solo oculta clientes "eliminados" (soft delete): los
        // identificamos porque al borrarlos les renombramos el email
        // del usuario a "...@borrado.local". Los clientes que el admin
        // haya marcado como inactivos editando siguen visibles para
        // poder reactivarlos.
        return clienteRepository.findAll().stream()
                .filter(c -> !esSoftDeleted(c))
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
                .filter(c -> !esSoftDeleted(c))
                .map(ClienteResponse::from)
                .toList();
    }

    /** Marca de cliente "eliminado" mediante soft delete (email renombrado). */
    private static boolean esSoftDeleted(Cliente c) {
        if (c.getUsuario() == null) return false;
        String email = c.getUsuario().getEmail();
        return email != null && email.endsWith("@borrado.local");
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

        // Si ya existe un usuario con ese email, comprobamos si es un
        // "huerfano" de un borrado antiguo (cuando aun se hacia hard
        // delete del cliente sin limpiar el usuario asociado). En ese
        // caso lo limpiamos sobre la marcha y permitimos el alta. Si no
        // es huerfano (lo usa otro cliente / admin / empleado activo),
        // rechazamos como antes.
        usuarioRepository.findByEmail(req.email()).ifPresent(existente -> {
            boolean tieneCliente = clienteRepository
                    .findByUsuarioId(existente.getId()).isPresent();
            boolean esCandidatoLimpieza =
                    !tieneCliente
                    && existente.getRol() == Rol.CLIENTE;
            if (esCandidatoLimpieza) {
                log.info("Limpieza de usuario huerfano (sin cliente) id={}, email={}",
                        existente.getId(), existente.getEmail());
                usuarioRepository.delete(existente);
                usuarioRepository.flush();
            } else {
                throw new IllegalArgumentException(
                    "Ya existe un usuario con el email: " + req.email());
            }
        });
        if (req.nif() != null && !req.nif().isBlank()
                && clienteRepository.existsByNif(req.nif())) {
            throw new IllegalArgumentException(
                "Ya existe un cliente con el NIF: " + req.nif());
        }

        String passwordTemporal = PasswordGenerator.generar();
        String passwordHash = passwordEncoder.encode(passwordTemporal);

        Usuario usuario = Usuario.builder()
                .email(req.email())
                .passwordHash(passwordHash)
                .nombre(req.nombre())
                .apellidos(req.apellidos())
                .rol(Rol.CLIENTE)
                .activo(true)
                .build();

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

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.CLIENTE_CREADO, "Cliente",
            guardado.getId(), securityHelper.getUsuarioActualId(),
            "Alta de cliente: " + req.email()));

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

        if (req.nif() != null && !req.nif().isBlank()
                && !req.nif().equals(cliente.getNif())
                && clienteRepository.existsByNif(req.nif())) {
            throw new IllegalArgumentException(
                "Ya existe otro cliente con el NIF: " + req.nif());
        }

        Usuario u = cliente.getUsuario();
        u.setNombre(req.nombre());
        u.setApellidos(req.apellidos());
        if (req.activo() != null) {
            u.setActivo(req.activo());
        }

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

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.CLIENTE_ACTUALIZADO, "Cliente",
            id, securityHelper.getUsuarioActualId(),
            "Actualizacion de cliente id=" + id));

        return ClienteResponse.from(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", id));

        Usuario usuario = cliente.getUsuario();
        String emailOriginal = (usuario != null) ? usuario.getEmail() : "(sin usuario)";

        // Si el cliente NO tiene pedidos podemos hacer un borrado real
        // (cliente + usuario). En caso contrario, las FK de pedidos
        // impedirian el delete, asi que hacemos "soft delete":
        //   - liberamos el email del usuario renombrandolo
        //   - liberamos el NIF del cliente
        //   - marcamos el usuario como inactivo
        // Asi el admin puede dar de alta otro cliente con el mismo email
        // o el mismo NIF sin perder el historial de pedidos.
        boolean tienePedidos = !pedidoRepository
                .findByClienteIdOrderByFechaPedidoDesc(id).isEmpty();

        if (!tienePedidos) {
            clienteRepository.delete(cliente);
            log.info("Cliente eliminado (hard): id={}, email={}", id, emailOriginal);
            auditoria.publish(AuditoriaEvent.entidad(
                    TipoEvento.CLIENTE_ELIMINADO, "Cliente",
                    id, securityHelper.getUsuarioActualId(),
                    "Eliminacion de cliente: " + emailOriginal));
            return;
        }

        // Soft delete: tiene pedidos historicos, no podemos perder esa info.
        if (usuario != null) {
            String emailLiberado = "borrado-" + usuario.getId()
                    + "-" + System.currentTimeMillis() + "@borrado.local";
            usuario.setEmail(emailLiberado);
            usuario.setActivo(false);
            usuarioRepository.save(usuario);
        }
        cliente.setNif(null); // libera el NIF para que se pueda reutilizar
        clienteRepository.save(cliente);

        log.info("Cliente eliminado (soft, tiene pedidos): id={}, emailOriginal={}",
                id, emailOriginal);
        auditoria.publish(AuditoriaEvent.entidad(
                TipoEvento.CLIENTE_ELIMINADO, "Cliente",
                id, securityHelper.getUsuarioActualId(),
                "Cliente desactivado (tenia pedidos asociados): " + emailOriginal));
    }

    private String nullSiVacio(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}