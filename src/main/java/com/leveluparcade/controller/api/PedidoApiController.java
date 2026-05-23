package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.PagarPedidoRequest;
import com.leveluparcade.dto.request.PedidoCreateRequest;
import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.EstadoPedido;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * API REST para la gestion de pedidos.
 *
 * <p>A diferencia de Clientes/Proveedores, este controller permite tanto
 * ADMIN como CLIENTE. Las reglas:
 * <ul>
 *   <li>Listar todos: solo ADMIN</li>
 *   <li>Listar los mios: cualquier autenticado (mostrara los del cliente logueado)</li>
 *   <li>Obtener uno: ADMIN siempre, CLIENTE solo si es suyo</li>
 *   <li>Crear: ambos. El CLIENTE no puede especificar otro clienteId</li>
 *   <li>Pagar: ambos. CLIENTE solo si es suyo</li>
 *   <li>Enviar / Entregar: solo ADMIN</li>
 *   <li>Cancelar: ADMIN siempre, CLIENTE solo si es suyo y esta PENDIENTE</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoApiController {

    private final PedidoService pedidoService;
    private final ClienteRepository clienteRepository;
    private final SecurityHelper securityHelper;

    public PedidoApiController(PedidoService pedidoService,
                               ClienteRepository clienteRepository,
                               SecurityHelper securityHelper) {
        this.pedidoService = pedidoService;
        this.clienteRepository = clienteRepository;
        this.securityHelper = securityHelper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<PedidoResponse> listarTodos(
            @RequestParam(required = false) EstadoPedido estado) {
        if (estado != null) {
            return pedidoService.listarPorEstado(estado);
        }
        return pedidoService.listarTodos();
    }

    /** Pedidos del cliente autenticado. */
    @GetMapping("/mios")
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMIN')")
    public List<PedidoResponse> listarMios() {
        Long clienteId = clienteIdDelUsuarioActual();
        return pedidoService.listarDeCliente(clienteId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PedidoResponse obtener(@PathVariable Long id) {
        PedidoResponse pedido = pedidoService.obtenerPorId(id);
        verificarPropiedadOAdmin(pedido.clienteId());
        return pedido;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE') or hasRole('ADMIN')")
    public ResponseEntity<PedidoResponse> crear(
            @Valid @RequestBody PedidoCreateRequest request) {

        // ADMIN puede especificar clienteId. CLIENTE usa el suyo (ignora el campo).
        Long clienteId = esAdmin() && request.clienteId() != null
                ? request.clienteId()
                : clienteIdDelUsuarioActual();

        PedidoResponse creado = pedidoService.crear(clienteId, request);

        URI location = URI.create("/api/pedidos/" + creado.id());
        return ResponseEntity.created(location).body(creado);
    }

    @PostMapping("/{id}/pagar")
    @PreAuthorize("isAuthenticated()")
    public PedidoResponse pagar(@PathVariable Long id,
                                @Valid @RequestBody PagarPedidoRequest request) {
        PedidoResponse pedido = pedidoService.obtenerPorId(id);
        verificarPropiedadOAdmin(pedido.clienteId());
        return pedidoService.pagar(id, request);
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasRole('ADMIN')")
    public PedidoResponse enviar(@PathVariable Long id) {
        return pedidoService.enviar(id);
    }

    @PostMapping("/{id}/entregar")
    @PreAuthorize("hasRole('ADMIN')")
    public PedidoResponse entregar(@PathVariable Long id) {
        return pedidoService.entregar(id);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public PedidoResponse cancelar(@PathVariable Long id) {
        PedidoResponse pedido = pedidoService.obtenerPorId(id);
        verificarPropiedadOAdmin(pedido.clienteId());
        return pedidoService.cancelar(id);
    }

    // ---------- helpers ----------

    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    /**
     * Lanza AccessDeniedException si el usuario actual no es admin
     * y el pedido no es suyo.
     */
    private void verificarPropiedadOAdmin(Long clienteIdDelPedido) {
        if (esAdmin()) {
            return;
        }
        Long clienteIdActual = clienteIdDelUsuarioActual();
        if (!clienteIdDelPedido.equals(clienteIdActual)) {
            throw new AccessDeniedException(
                "No tienes acceso a este pedido");
        }
    }

    /**
     * Devuelve el id del Cliente asociado al usuario autenticado.
     * @throws AccessDeniedException si el usuario no tiene cliente asociado
     */
    private Long clienteIdDelUsuarioActual() {
        Long usuarioId = securityHelper.getUsuarioActualId();
        if (usuarioId == null) {
            throw new AccessDeniedException("No autenticado");
        }
        Cliente cliente = clienteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "El usuario autenticado no tiene un perfil de cliente asociado"));
        return cliente.getId();
    }
}