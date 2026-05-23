package com.leveluparcade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Una linea (producto + cantidad) dentro de un pedido.
 *
 * <p>El {@code precio_unitario} se copia del producto en el momento del
 * pedido y NO se actualiza si el precio del producto cambia despues:
 * los pedidos antiguos mantienen el precio que tenian al crearse
 * (trazabilidad de facturacion).
 *
 * Tabla asociada: {@code lineas_pedido} (definida en V1__init.sql,
 * con ON DELETE CASCADE hacia pedidos).
 */
@Entity
@Table(name = "lineas_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"pedido", "producto"})
public class LineaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull
    @Positive
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @NotNull
    @PositiveOrZero
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    /** Subtotal calculado al vuelo: cantidad * precio_unitario. */
    public BigDecimal getSubtotal() {
        if (precioUnitario == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}