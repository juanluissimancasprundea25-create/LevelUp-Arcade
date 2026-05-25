package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.AjusteStockRequest;
import com.leveluparcade.dto.request.ProductoCreateRequest;
import com.leveluparcade.dto.request.ProductoUpdateRequest;
import com.leveluparcade.dto.response.ProductoResponse;
import com.leveluparcade.service.ProductoService;
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
 * API REST para la gestion de productos desde el panel admin.
 *
 * <p>Lectura abierta a ADMIN y EMPLEADO. Escritura, ajuste de stock y
 * borrado restringidos a ADMIN.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoApiController {

    private final ProductoService productoService;

    public ProductoApiController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public List<ProductoResponse> listar(
            @RequestParam(value = "q", required = false) String texto,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "proveedorId", required = false) Long proveedorId,
            @RequestParam(value = "bajoStock", required = false) Boolean bajoStock) {

        if (Boolean.TRUE.equals(bajoStock)) {
            return productoService.listarBajoStock();
        }
        if (categoriaId != null) {
            return productoService.listarPorCategoria(categoriaId);
        }
        if (proveedorId != null) {
            return productoService.listarPorProveedor(proveedorId);
        }
        if (texto != null && !texto.isBlank()) {
            return productoService.buscarPorTexto(texto);
        }
        return productoService.listarTodos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> crear(
            @Valid @RequestBody ProductoCreateRequest request) {

        ProductoResponse creado = productoService.crear(request);
        URI location = URI.create("/api/productos/" + creado.id());
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUpdateRequest request) {
        return productoService.actualizar(id, request);
    }

    @PostMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse ajustarStock(
            @PathVariable Long id,
            @Valid @RequestBody AjusteStockRequest request) {
        return productoService.ajustarStock(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}