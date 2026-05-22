package com.leveluparcade.controller.api;

import com.leveluparcade.entity.Producto;
import com.leveluparcade.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoApiController {

    private final ProductoService productoService;

    public ProductoApiController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<Producto> listar() {
        return productoService.listarTodos();
    }

    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Long id) {
        return productoService.obtenerPorId(id);
    }

    @PostMapping
    public Producto crear(@RequestBody @Valid Producto producto) {
        return productoService.guardar(producto);
    }

    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id,
                               @RequestBody @Valid Producto producto) {
        return productoService.actualizar(id, producto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}