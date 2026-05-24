package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.CrearDevolucionRequest;
import com.leveluparcade.dto.request.RechazarDevolucionRequest;
import com.leveluparcade.dto.response.DevolucionResponse;
import com.leveluparcade.entity.EstadoDevolucion;
import com.leveluparcade.service.DevolucionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/devoluciones")
public class DevolucionApiController {

    private final DevolucionService service;

    public DevolucionApiController(DevolucionService service) {
        this.service = service;
    }

    /**
     * Crear devolucion: ADMIN o CLIENTE (el cliente solo sobre sus pedidos).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<DevolucionResponse> crear(
            @Valid @RequestBody CrearDevolucionRequest request) {
        DevolucionResponse creada = service.crearDevolucion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /**
     * Listado paginado con filtros (ADMIN).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<DevolucionResponse>> listar(
            @RequestParam(required = false) EstadoDevolucion estado,
            @RequestParam(required = false) Long clienteId,
            Pageable pageable) {
        return ResponseEntity.ok(service.listarDevoluciones(estado, clienteId, pageable));
    }

    /**
     * Devoluciones del cliente autenticado.
     */
    @GetMapping("/mias")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Page<DevolucionResponse>> listarMias(Pageable pageable) {
        return ResponseEntity.ok(service.listarMisDevoluciones(pageable));
    }

    /**
     * Detalle (ADMIN o dueno; la propiedad se verifica en el service).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    public ResponseEntity<DevolucionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerDevolucion(id));
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionResponse> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(service.aprobarDevolucion(id));
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionResponse> rechazar(
            @PathVariable Long id,
            @Valid @RequestBody RechazarDevolucionRequest request) {
        return ResponseEntity.ok(service.rechazarDevolucion(id, request));
    }

    @PostMapping("/{id}/completar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DevolucionResponse> completar(@PathVariable Long id) {
        return ResponseEntity.ok(service.completarDevolucion(id));
    }
}