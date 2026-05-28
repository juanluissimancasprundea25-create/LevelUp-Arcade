package com.leveluparcade.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carrito de compra persistente. Hay como maximo UNO por cliente
 * (relacion 1:1). Las lineas se gestionan en cascada: al borrar el
 * carrito se borran sus lineas.
 *
 * <p>El carrito NO es un pedido: es el paso previo. El pedido se crea
 * a partir del carrito en el checkout (PR #24).
 *
 * Tabla asociada: {@code carritos} (V4__add_carrito.sql).
 */
@Entity
@Table(name = "carritos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"lineas", "cliente"})
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LineaCarrito> lineas = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    /** Numero total de unidades (suma de cantidades de todas las lineas). */
    public int getTotalUnidades() {
        if (lineas == null) {
            return 0;
        }
        return lineas.stream()
                .mapToInt(l -> l.getCantidad() != null ? l.getCantidad() : 0)
                .sum();
    }

    /** Importe total del carrito (suma de subtotales de cada linea). */
    public BigDecimal getTotal() {
        if (lineas == null) {
            return BigDecimal.ZERO;
        }
        return lineas.stream()
                .map(LineaCarrito::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** True si el carrito no tiene lineas. */
    public boolean estaVacio() {
        return lineas == null || lineas.isEmpty();
    }
}
