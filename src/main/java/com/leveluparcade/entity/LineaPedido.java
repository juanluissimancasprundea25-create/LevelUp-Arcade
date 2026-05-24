package com.leveluparcade.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lineas_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    /**
     * Acumulador de unidades ya devueltas de esta linea.
     * Se incrementa al aprobar una devolucion y nunca supera 'cantidad'.
     * Anadido en V3.
     */
    @Builder.Default
    @Column(name = "cantidad_devuelta", nullable = false)
    private Integer cantidadDevuelta = 0;

    // === Logica de negocio ===
    public BigDecimal getSubtotal() {
        if (precioUnitario == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    /**
     * Unidades aun devolvibles (no devueltas todavia).
     */
    public int getCantidadDevolvible() {
        int devuelta = cantidadDevuelta == null ? 0 : cantidadDevuelta;
        return cantidad - devuelta;
    }
}