package com.leveluparcade.service;

import com.leveluparcade.entity.Categoria;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.CategoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    public Categoria obtenerPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Categoría no encontrada con id: " + id));
    }

    public Categoria guardar(Categoria categoria) {

        if (categoriaRepository.existsByNombre(categoria.getNombre())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        }

        return categoriaRepository.save(categoria);
    }

    public Categoria actualizar(Long id, Categoria datos) {

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Categoría no encontrada con id: " + id));

        if (!categoria.getNombre().equals(datos.getNombre()) &&
                categoriaRepository.existsByNombre(datos.getNombre())) {
            throw new RuntimeException("Ya existe una categoría con ese nombre");
        }

        categoria.setNombre(datos.getNombre());
        categoria.setDescripcion(datos.getDescripcion());

        return categoriaRepository.save(categoria);
    }

    public void eliminar(Long id) {

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Categoría no encontrada con id: " + id));

        categoriaRepository.delete(categoria);
    }
}