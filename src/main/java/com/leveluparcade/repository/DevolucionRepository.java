package com.leveluparcade.repository;

import com.leveluparcade.entity.Devolucion;
import com.leveluparcade.entity.EstadoDevolucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {

    /**
     * Listado paginado con filtros opcionales (todos pueden ir nulos).
     * Pasamos el enum como String para evitar problemas de tipado JPQL con nulls.
     */
    @Query("""
        SELECT d FROM Devolucion d
        WHERE (:estado IS NULL OR d.estado = :estado)
          AND (:clienteId IS NULL OR d.pedido.cliente.id = :clienteId)
        ORDER BY d.fechaSolicitud DESC
        """)
    Page<Devolucion> buscarConFiltros(@Param("estado") EstadoDevolucion estado,
                                     @Param("clienteId") Long clienteId,
                                     Pageable pageable);

    /**
     * Devoluciones de un cliente concreto (endpoint /mias).
     */
    @Query("""
        SELECT d FROM Devolucion d
        WHERE d.pedido.cliente.id = :clienteId
        ORDER BY d.fechaSolicitud DESC
        """)
    Page<Devolucion> findByClienteId(@Param("clienteId") Long clienteId, Pageable pageable);

    /**
     * Todas las devoluciones de un pedido (para validaciones de negocio).
     */
    List<Devolucion> findByPedidoId(Long pedidoId);
}