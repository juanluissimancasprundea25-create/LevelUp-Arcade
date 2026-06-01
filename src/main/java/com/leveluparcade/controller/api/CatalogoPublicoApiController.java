package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.RegistroClienteCockpitRequest;
import com.leveluparcade.dto.response.CategoriaResponse;
import com.leveluparcade.dto.response.ProductoResponse;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.Rol;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.service.CategoriaService;
import com.leveluparcade.service.ProductoService;
import com.leveluparcade.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Endpoints publicos sin autenticacion para la tienda online (cockpit SPA).
 *
 * <p>Expone solo lo necesario para que un visitante anonimo pueda:
 * <ul>
 *   <li>Ver el catalogo de productos activos</li>
 *   <li>Ver el detalle de un producto</li>
 *   <li>Ver las categorias activas (para filtrar)</li>
 *   <li>Registrarse como nuevo cliente</li>
 * </ul>
 *
 * <p>Para que sean realmente publicos hay que tener
 * {@code .requestMatchers("/api/publico/**").permitAll()} en
 * {@code SecurityConfig.apiSecurityFilterChain}.
 *
 * <p>Convive con la version Thymeleaf clasica de
 * {@link com.leveluparcade.controller.web.TiendaAuthController}: cada una
 * usa su propio DTO ({@link RegistroClienteCockpitRequest} aqui,
 * {@link com.leveluparcade.dto.request.RegistroClienteRequest} alli).
 */
@RestController
@RequestMapping("/api/publico")
public class CatalogoPublicoApiController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public CatalogoPublicoApiController(ProductoService productoService,
                                         CategoriaService categoriaService,
                                         UsuarioRepository usuarioRepository,
                                         ClienteRepository clienteRepository,
                                         PasswordEncoder passwordEncoder,
                                         JwtService jwtService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ---------- CATALOGO ----------

    @GetMapping("/productos")
    public List<ProductoResponse> listarProductos() {
        return productoService.listarTodos().stream()
            .filter(p -> Boolean.TRUE.equals(p.activo()))
            .toList();
    }

    @GetMapping("/productos/{id}")
    public ProductoResponse obtenerProducto(@PathVariable Long id) {
        ProductoResponse p = productoService.obtenerPorId(id);
        if (p == null || !Boolean.TRUE.equals(p.activo())) {
            throw new com.leveluparcade.exception.ResourceNotFoundException(
                "Producto no encontrado");
        }
        return p;
    }

    @GetMapping("/categorias")
    public List<CategoriaResponse> listarCategorias() {
        return categoriaService.listarTodas().stream()
            .filter(c -> Boolean.TRUE.equals(c.activa()))
            .toList();
    }

    // ---------- REGISTRO ----------

    @PostMapping("/registro")
    @Transactional
    public ResponseEntity<Map<String, Object>> registrar(
            @Valid @RequestBody RegistroClienteCockpitRequest req) {

        String emailNormalizado = req.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("mensaje", "Ya existe un usuario con ese email")
            );
        }

        if (req.nif() != null && !req.nif().isBlank()
                && clienteRepository.findByNif(req.nif().trim().toUpperCase()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("mensaje", "Ya existe un cliente con ese NIF")
            );
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(passwordEncoder.encode(req.password()));
        usuario.setNombre(req.nombre().trim());
        usuario.setApellidos(req.apellidos() != null ? req.apellidos().trim() : null);
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario = usuarioRepository.save(usuario);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        if (req.nif() != null && !req.nif().isBlank()) cliente.setNif(req.nif().trim().toUpperCase());
        if (req.telefono() != null && !req.telefono().isBlank()) cliente.setTelefono(req.telefono().trim());
        if (req.direccion() != null && !req.direccion().isBlank()) cliente.setDireccion(req.direccion().trim());
        if (req.ciudad() != null && !req.ciudad().isBlank()) cliente.setCiudad(req.ciudad().trim());
        if (req.codigoPostal() != null && !req.codigoPostal().isBlank()) cliente.setCodigoPostal(req.codigoPostal().trim());
        if (req.pais() != null && !req.pais().isBlank()) cliente.setPais(req.pais().trim());
        cliente.setFechaAlta(LocalDateTime.now());
        clienteRepository.save(cliente);

        String token = jwtService.generateToken(usuario.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "token", token,
            "email", usuario.getEmail(),
            "nombre", usuario.getNombre(),
            "rol", usuario.getRol().name()
        ));
    }
}
