package com.leveluparcade.repository;

import com.leveluparcade.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Producto}.
 *
 * <p>Hereda los CRUD basicos de {@link JpaRepository} y queries
 * dinamicas paginadas via {@link JpaSpecificationExecutor} (usadas
 * por el catalogo publico). Anade tambien queries especificas para
 * busquedas y agrupaciones tipicas del panel admin.
 */
@Repository
public interface ProductoRepository
        extends JpaRepository<Producto, Long>,
                JpaSpecificationExecutor<Producto> {

    Optional<Producto> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Producto> findByCategoriaId(Long categoriaId);

    List<Producto> findByProveedorId(Long proveedorId);

    List<Producto> findByActivoTrue();

    /** Cuenta cuantos productos referencian a la categoria indicada. */
    long countByCategoriaId(Long categoriaId);

    /** Cuenta cuantos productos referencian al proveedor indicado. */
    long countByProveedorId(Long proveedorId);

    /**
     * Productos con stock por debajo o igual a su stock minimo.
     * Util para alertas de reposicion en el panel admin.
     */
    @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo AND p.activo = true")
    List<Producto> findBajoStock();

    /** Busqueda de texto libre en SKU, nombre o descripcion. */
    @Query("""
        SELECT p FROM Producto p
        WHERE LOWER(p.sku) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))
        """)
    List<Producto> buscarPorTexto(@Param("texto") String texto);
}
