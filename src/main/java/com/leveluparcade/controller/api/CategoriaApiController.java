package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.CategoriaCreateRequest;
import com.leveluparcade.dto.request.CategoriaUpdateRequest;
import com.leveluparcade.dto.response.CategoriaResponse;
import com.leveluparcade.service.CategoriaService;
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
 * API REST para la gestion de categorias desde el panel admin.
 *
 * <p>Lectura abierta a ADMIN y EMPLEADO (para poder elegir categoria
 * al editar productos). Escritura solo ADMIN.
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaApiController {

    private final CategoriaService categoriaService;

    public CategoriaApiController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public List<CategoriaResponse> listar(
            @RequestParam(value = "q", required = false) String texto) {
        if (texto == null || texto.isBlank()) {
            return categoriaService.listarTodas();
        }
        return categoriaService.buscarPorTexto(texto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public CategoriaResponse obtener(@PathVariable Long id) {
        return categoriaService.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> crear(
            @Valid @RequestBody CategoriaCreateRequest request) {

        CategoriaResponse creada = categoriaService.crear(request);
        URI location = URI.create("/api/categorias/" + creada.id());
        return ResponseEntity.created(location).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoriaResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaUpdateRequest request) {
        return categoriaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
    }
}