package com.leveluparcade.service;

import com.leveluparcade.entity.Producto;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    public Producto guardar(Producto producto) {

        if (productoRepository.existsBySku(producto.getSku())) {
            throw new RuntimeException("Ya existe un producto con ese SKU");
        }

        return productoRepository.save(producto);
    }

    public Producto actualizar(Long id, Producto datos) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Producto no encontrado con id: " + id));

        if (!producto.getSku().equals(datos.getSku()) &&
                productoRepository.existsBySku(datos.getSku())) {
            throw new RuntimeException("Ya existe un producto con ese SKU");
        }

        producto.setSku(datos.getSku());
        producto.setNombre(datos.getNombre());
        producto.setDescripcion(datos.getDescripcion());
        producto.setPrecio(datos.getPrecio());
        producto.setStock(datos.getStock());
        producto.setStockMinimo(datos.getStockMinimo());
        producto.setCategoria(datos.getCategoria());
        producto.setImagenUrl(datos.getImagenUrl());
        producto.setActivo(datos.getActivo());

        return productoRepository.save(producto);
    }

    public void eliminar(Long id) {

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Producto no encontrado con id: " + id));

        productoRepository.delete(producto);
    }
}