package com.leveluparcade.repository;

import com.leveluparcade.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio del {@link Carrito}. Como hay 1 carrito por cliente,
 * la busqueda principal es por el cliente (o por el email de su usuario).
 */
@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    /** Carrito de un cliente por su id. */
    Optional<Carrito> findByClienteId(Long clienteId);

    /** Carrito de un cliente por el email de su usuario asociado. */
    Optional<Carrito> findByClienteUsuarioEmail(String email);

    /**
     * Carrito de un cliente con sus lineas y productos ya cargados
     * (LEFT JOIN FETCH) para poder renderizar la vista con
     * open-in-view=false sin LazyInitializationException.
     *
     * <p>LEFT porque un carrito puede estar vacio (sin lineas).
     */
    @Query("""
        SELECT DISTINCT c FROM Carrito c
        LEFT JOIN FETCH c.lineas l
        LEFT JOIN FETCH l.producto
        WHERE c.cliente.id = :clienteId
        """)
    Optional<Carrito> findByClienteIdConLineas(@Param("clienteId") Long clienteId);
}
