package com.leveluparcade.repository;

import com.leveluparcade.entity.Rol;
import com.leveluparcade.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Usuario}.
 *
 * <p>Spring Data JPA genera automáticamente la implementación de todos los
 * métodos heredados de {@link JpaRepository} (findAll, findById, save, deleteById,
 * count, etc.) y de los métodos declarados aquí siguiendo la convención de
 * nombres ("query methods").</p>
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su email (único en la BD).
     * Útil para el login y para comprobar duplicados al registrar.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Comprueba si existe ya un usuario con ese email.
     * Más eficiente que {@code findByEmail(email).isPresent()} porque
     * Hibernate genera SELECT COUNT en lugar de cargar la entidad.
     */
    boolean existsByEmail(String email);

    /**
     * Lista todos los usuarios con un rol concreto (ADMIN, EMPLEADO, CLIENTE).
     */
    List<Usuario> findByRol(Rol rol);

    /**
     * Lista solo los usuarios activos con un rol concreto.
     */
    List<Usuario> findByRolAndActivoTrue(Rol rol);
}