package com.leveluparcade.repository;

import com.leveluparcade.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Cliente}.
 *
 * <p>Hereda de {@link JpaRepository} los métodos CRUD básicos
 * (findAll, findById, save, deleteById, count, etc.).</p>
 *
 * <p>Define queries específicas de cliente que necesitará el módulo de
 * gestión: búsqueda por NIF, por usuario asociado, por ciudad, y un
 * buscador de texto libre sobre nombre/email/NIF.</p>
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Busca un cliente por el ID del usuario asociado.
     * Como la relación es 1:1, devuelve como máximo un resultado.
     */
    Optional<Cliente> findByUsuarioId(Long usuarioId);

    /**
     * Busca un cliente por el email de su usuario asociado.
     * Útil porque normalmente identificamos al cliente por su email de login.
     */
    Optional<Cliente> findByUsuarioEmail(String email);

    /**
     * Busca un cliente por su NIF (único en la BD).
     */
    Optional<Cliente> findByNif(String nif);

    /**
     * Comprueba si existe ya un cliente con ese NIF.
     */
    boolean existsByNif(String nif);

    /**
     * Lista todos los clientes de una ciudad concreta.
     */
    List<Cliente> findByCiudadIgnoreCase(String ciudad);

    /**
     * Buscador de texto libre: encuentra clientes cuyo nombre, apellidos,
     * email o NIF contengan el texto (ignorando mayúsculas).
     *
     * <p>Útil para el buscador del panel de administración.</p>
     */
    @Query("""
        SELECT c FROM Cliente c
        WHERE LOWER(c.usuario.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(c.usuario.apellidos) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(c.usuario.email) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(c.nif) LIKE LOWER(CONCAT('%', :texto, '%'))
        """)
    List<Cliente> buscarPorTexto(@Param("texto") String texto);
}