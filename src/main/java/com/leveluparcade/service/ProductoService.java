package com.leveluparcade.service;

import com.leveluparcade.dto.request.AjusteStockRequest;
import com.leveluparcade.dto.request.ProductoCreateRequest;
import com.leveluparcade.dto.request.ProductoUpdateRequest;
import com.leveluparcade.dto.response.ProductoResponse;

import java.util.List;

/**
 * Operaciones de gestion de productos desde el panel admin.
 */
public interface ProductoService {

    List<ProductoResponse> listarTodos();

    List<ProductoResponse> buscarPorTexto(String texto);

    List<ProductoResponse> listarPorCategoria(Long categoriaId);

    List<ProductoResponse> listarPorProveedor(Long proveedorId);

    List<ProductoResponse> listarBajoStock();

    ProductoResponse obtenerPorId(Long id);

    /**
     * Crea un nuevo producto.
     * @throws IllegalArgumentException si el SKU ya existe
     * @throws com.leveluparcade.exception.ResourceNotFoundException
     *         si la categoria o el proveedor referenciado no existe
     */
    ProductoResponse crear(ProductoCreateRequest request);

    /**
     * Actualiza un producto existente.
     */
    ProductoResponse actualizar(Long id, ProductoUpdateRequest request);

    /**
     * Ajusta manualmente el stock (entrada / salida).
     * Una cantidad positiva incrementa, negativa decrementa.
     * @throws IllegalStateException si el ajuste deja el stock en negativo
     */
    ProductoResponse ajustarStock(Long id, AjusteStockRequest request);

    /**
     * Elimina un producto.
     * @throws IllegalStateException si tiene lineas de pedido asociadas
     */
    void eliminar(Long id);
}