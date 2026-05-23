package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.ClienteCreateRequest;
import com.leveluparcade.dto.request.ClienteUpdateRequest;
import com.leveluparcade.dto.response.ClienteCreadoResponse;
import com.leveluparcade.dto.response.ClienteResponse;
import com.leveluparcade.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * API REST para la gestion de clientes desde el panel admin.
 *
 * <p>Todos los endpoints requieren rol ADMIN. Las respuestas siguen el
 * estandar REST: 200 OK para lecturas exitosas, 201 Created con Location
 * para creacion, 204 No Content para borrado, y 4xx para errores
 * mapeados por {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasRole('ADMIN')")
public class ClienteApiController {

    private final ClienteService clienteService;

    public ClienteApiController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    /**
     * Lista todos los clientes, o filtra por texto libre si se pasa
     * el parametro {@code q} (busqueda en nombre/apellidos/email/NIF).
     */
    @GetMapping
    public List<ClienteResponse> listar(
            @RequestParam(value = "q", required = false) String texto) {
        if (texto == null || texto.isBlank()) {
            return clienteService.listarTodos();
        }
        return clienteService.buscarPorTexto(texto);
    }

    /** Obtiene un cliente por id. */
    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id) {
        return clienteService.obtenerPorId(id);
    }

    /**
     * Da de alta un nuevo cliente. Devuelve 201 Created con la entidad
     * creada mas la password temporal generada (visible UNA sola vez).
     */
    @PostMapping
    public ResponseEntity<ClienteCreadoResponse> crear(
            @Valid @RequestBody ClienteCreateRequest request) {

        ClienteCreadoResponse creado = clienteService.crear(request);

        URI location = URI.create("/api/clientes/" + creado.cliente().id());
        return ResponseEntity.created(location).body(creado);
    }

    /** Actualiza un cliente existente. */
    @PutMapping("/{id}")
    public ClienteResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateRequest request) {
        return clienteService.actualizar(id, request);
    }

    /** Elimina un cliente (y por cascada, su Usuario). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
    }
}