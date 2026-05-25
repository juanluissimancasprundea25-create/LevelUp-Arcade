package com.leveluparcade.service;

import com.leveluparcade.dto.request.CategoriaCreateRequest;
import com.leveluparcade.dto.request.CategoriaUpdateRequest;
import com.leveluparcade.dto.response.CategoriaResponse;

import java.util.List;

/**
 * Operaciones de gestion de categorias desde el panel admin.
 */
public interface CategoriaService {

    List<CategoriaResponse> listarTodas();

    List<CategoriaResponse> buscarPorTexto(String texto);

    CategoriaResponse obtenerPorId(Long id);

    /**
     * Crea una nueva categoria.
     * @throws IllegalArgumentException si el nombre ya existe
     */
    CategoriaResponse crear(CategoriaCreateRequest request);

    /**
     * Actualiza una categoria existente. Permite renombrarla si el nuevo
     * nombre no esta ocupado.
     */
    CategoriaResponse actualizar(Long id, CategoriaUpdateRequest request);

    /**
     * Elimina una categoria.
     * @throws IllegalStateException si tiene productos asociados
     */
    void eliminar(Long id);
}