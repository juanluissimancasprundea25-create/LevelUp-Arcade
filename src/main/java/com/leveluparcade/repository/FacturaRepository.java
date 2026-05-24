package com.leveluparcade.repository;

import com.leveluparcade.entity.Factura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByPedidoId(Long pedidoId);

    boolean existsByPedidoId(Long pedidoId);

    Optional<Factura> findByNumeroFactura(String numeroFactura);

    /**
     * Devuelve el último número secuencial usado en un año concreto.
     * Sirve para generar el siguiente número de factura.
     * Usa LIKE sobre numero_factura con el patrón "{prefijo}-{año}-%".
     */
    @Query("""
        SELECT COALESCE(MAX(CAST(SUBSTRING(f.numeroFactura, LENGTH(:prefijoYAnio) + 1) AS int)), 0)
        FROM Factura f
        WHERE f.numeroFactura LIKE CONCAT(:prefijoYAnio, '%')
    """)
    Integer findUltimoSecuencialPorPrefijoAnio(@Param("prefijoYAnio") String prefijoYAnio);

    @Query("""
        SELECT f FROM Factura f
        WHERE (:clienteId IS NULL OR f.pedido.cliente.id = :clienteId)
          AND (:desde IS NULL OR f.fechaEmision >= :desde)
          AND (:hasta IS NULL OR f.fechaEmision <= :hasta)
    """)
    Page<Factura> buscarConFiltros(
        @Param("clienteId") Long clienteId,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        Pageable pageable
    );

    Page<Factura> findByPedidoClienteId(Long clienteId, Pageable pageable);
}