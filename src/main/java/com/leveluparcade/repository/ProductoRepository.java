package com.leveluparcade.repository;

import com.leveluparcade.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySku(String sku);

    List<Producto> findByCategoriaId(Long categoriaId);

    boolean existsBySku(String sku);

    /** Cuenta cuantos productos referencian al proveedor indicado. */
    long countByProveedorId(Long proveedorId);
}