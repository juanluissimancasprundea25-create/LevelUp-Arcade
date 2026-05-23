package com.leveluparcade.repository;

import com.leveluparcade.entity.EstadoPedido;
import com.leveluparcade.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para la entidad {@link Pedido}.
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /** Pedidos de un cliente, mas recientes primero. */
    List<Pedido> findByClienteIdOrderByFechaPedidoDesc(Long clienteId);

    /** Pedidos por estado, mas recientes primero. */
    List<Pedido> findByEstadoOrderByFechaPedidoDesc(EstadoPedido estado);
}