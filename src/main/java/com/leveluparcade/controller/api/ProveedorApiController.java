package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.ProveedorCreateRequest;
import com.leveluparcade.dto.request.ProveedorUpdateRequest;
import com.leveluparcade.dto.response.ProveedorResponse;
import com.leveluparcade.service.ProveedorService;
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
 * API REST para la gestion de proveedores desde el panel admin.
 *
 * <p>Todos los endpoints requieren rol ADMIN.
 */
@RestController
@RequestMapping("/api/proveedores")
@PreAuthorize("hasRole('ADMIN')")
public class ProveedorApiController {

    private final ProveedorService proveedorService;

    public ProveedorApiController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public List<ProveedorResponse> listar(
            @RequestParam(value = "q", required = false) String texto) {
        if (texto == null || texto.isBlank()) {
            return proveedorService.listarTodos();
        }
        return proveedorService.buscarPorTexto(texto);
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return proveedorService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(
            @Valid @RequestBody ProveedorCreateRequest request) {

        ProveedorResponse creado = proveedorService.crear(request);
        URI location = URI.create("/api/proveedores/" + creado.id());
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorUpdateRequest request) {
        return proveedorService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        proveedorService.eliminar(id);
    }
}