package com.leveluparcade.service;

import com.leveluparcade.dto.request.ProveedorCreateRequest;
import com.leveluparcade.dto.request.ProveedorUpdateRequest;
import com.leveluparcade.dto.response.ProveedorResponse;

import java.util.List;

/**
 * Operaciones de gestion de proveedores desde el panel admin.
 */
public interface ProveedorService {

    List<ProveedorResponse> listarTodos();

    List<ProveedorResponse> buscarPorTexto(String texto);

    ProveedorResponse obtenerPorId(Long id);

    /**
     * Crea un nuevo proveedor.
     * @throws IllegalArgumentException si el CIF ya existe
     */
    ProveedorResponse crear(ProveedorCreateRequest request);

    /**
     * Actualiza un proveedor existente. Permite cambiar el CIF si el
     * nuevo no esta ocupado por otro proveedor.
     */
    ProveedorResponse actualizar(Long id, ProveedorUpdateRequest request);

    /**
     * Elimina un proveedor.
     * @throws IllegalStateException si el proveedor tiene productos asociados
     */
    void eliminar(Long id);
}