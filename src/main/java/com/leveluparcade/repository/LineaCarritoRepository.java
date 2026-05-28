package com.leveluparcade.repository;

import com.leveluparcade.entity.LineaCarrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de las {@link LineaCarrito}.
 */
@Repository
public interface LineaCarritoRepository extends JpaRepository<LineaCarrito, Long> {

    /** Busca la linea de un producto concreto dentro de un carrito. */
    Optional<LineaCarrito> findByCarritoIdAndProductoId(Long carritoId, Long productoId);
}
