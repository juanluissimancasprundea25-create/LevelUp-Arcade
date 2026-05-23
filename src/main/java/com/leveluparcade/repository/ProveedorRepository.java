package com.leveluparcade.repository;

import com.leveluparcade.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Proveedor}.
 *
 * <p>Hereda los CRUD basicos de {@link JpaRepository} (findAll, findById,
 * save, deleteById, count, etc.) y anade queries especificas para
 * busquedas comunes en el panel admin.
 */
@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    /** Busca un proveedor por CIF (unico en BD). */
    Optional<Proveedor> findByCif(String cif);

    /** Comprueba si existe un proveedor con ese CIF. */
    boolean existsByCif(String cif);

    /** Lista proveedores por ciudad (case-insensitive). */
    List<Proveedor> findByCiudadIgnoreCase(String ciudad);

    /** Lista solo proveedores activos. */
    List<Proveedor> findByActivoTrue();

    /**
     * Busqueda de texto libre en nombre_empresa, CIF, email o ciudad.
     * Usado por el buscador del panel admin.
     */
    @Query("""
        SELECT p FROM Proveedor p
        WHERE LOWER(p.nombreEmpresa) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(p.cif) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(p.emailContacto) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(p.ciudad) LIKE LOWER(CONCAT('%', :texto, '%'))
        """)
    List<Proveedor> buscarPorTexto(@Param("texto") String texto);
}