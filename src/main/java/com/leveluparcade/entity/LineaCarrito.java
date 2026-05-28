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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Linea de carrito: un producto con su cantidad dentro de un carrito.
 *
 * <p>El precio NO se guarda aqui: se lee del producto en el momento de
 * mostrar/checkout, asi el carrito siempre refleja el precio actual.
 * (El precio "congelado" se guardara en la linea de PEDIDO al hacer
 * checkout, no en el carrito.)
 *
 * Tabla asociada: {@code lineas_carrito} (V4__add_carrito.sql).
 */
@Entity
@Table(
    name = "lineas_carrito",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_carrito_producto",
        columnNames = {"carrito_id", "producto_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"carrito"})
public class LineaCarrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer cantidad;

    /** Subtotal de la linea: precio actual del producto * cantidad. */
    public BigDecimal getSubtotal() {
        if (producto == null || producto.getPrecio() == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));
    }
}
