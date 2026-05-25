package com.leveluparcade.repository;

import com.leveluparcade.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Categoria}.
 *
 * <p>Hereda los CRUD basicos de {@link JpaRepository} y anade queries
 * especificas para el panel admin.
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Busca una categoria por nombre exacto (unico en BD). */
    Optional<Categoria> findByNombre(String nombre);

    /** Comprueba si existe una categoria con ese nombre. */
    boolean existsByNombre(String nombre);

    /** Lista solo categorias activas. */
    List<Categoria> findByActivaTrue();

    /** Busqueda de texto libre en nombre o descripcion. */
    @Query("""
        SELECT c FROM Categoria c
        WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))
        """)
    List<Categoria> buscarPorTexto(@Param("texto") String texto);
}