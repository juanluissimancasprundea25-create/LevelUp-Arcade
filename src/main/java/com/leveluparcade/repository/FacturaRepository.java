package com.leveluparcade.repository;

import com.leveluparcade.entity.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByPedidoId(Long pedidoId);

    boolean existsByPedidoId(Long pedidoId);

    Optional<Factura> findByNumeroFactura(String numeroFactura);

    @Query("""
        SELECT f FROM Factura f
        WHERE f.numeroFactura LIKE CONCAT(:prefijoYAnio, '%')
        ORDER BY f.numeroFactura DESC
    """)
    List<Factura> buscarUltimaPorPrefijoAnio(
        @Param("prefijoYAnio") String prefijoYAnio,
        Pageable pageable
    );

    /**
     * Busqueda con filtros opcionales. Usa CAST sobre los parametros nulos
     * para que PostgreSQL pueda inferir el tipo cuando se pasa NULL
     * (evita el error SQLState 42P18).
     */
    @Query("""
        SELECT f FROM Factura f
        WHERE (CAST(:clienteId AS long) IS NULL OR f.pedido.cliente.id = :clienteId)
          AND (CAST(:desde AS timestamp) IS NULL OR f.fechaEmision >= :desde)
          AND (CAST(:hasta AS timestamp) IS NULL OR f.fechaEmision <= :hasta)
    """)
    Page<Factura> buscarConFiltros(
        @Param("clienteId") Long clienteId,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        Pageable pageable
    );

    Page<Factura> findByPedidoClienteId(Long clienteId, Pageable pageable);
}