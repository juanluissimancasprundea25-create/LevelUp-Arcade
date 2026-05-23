package com.leveluparcade.repository;

import com.leveluparcade.entity.LineaPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad {@link LineaPedido}.
 *
 * <p>Las lineas se gestionan principalmente a traves de su Pedido
 * (cascada), pero este repositorio existe para consultas independientes
 * (ej: estadisticas de productos mas vendidos).
 */
@Repository
public interface LineaPedidoRepository extends JpaRepository<LineaPedido, Long> {

    List<LineaPedido> findByPedidoId(Long pedidoId);

    /** Cuenta lineas que referencian a un producto (util si quieres bloquear borrado de producto). */
    long countByProductoId(Long productoId);
}