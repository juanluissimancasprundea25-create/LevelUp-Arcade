package com.leveluparcade.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un pedido realizado por un cliente.
 *
 * <p>Las lineas del pedido se gestionan en cascada: al persistir un
 * Pedido tambien se persisten sus lineas (CascadeType.ALL). Al borrar
 * un pedido, sus lineas se borran via ON DELETE CASCADE de la BD.
 *
 * <p>El total se calcula y persiste para no recalcularlo en cada query.
 * Se recalcula en el service cada vez que las lineas cambian.
 *
 * Tabla asociada: {@code pedidos} (definida en V1__init.sql).
 */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"lineas", "cliente"})
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @CreationTimestamp
    @Column(name = "fecha_pedido", nullable = false, updatable = false)
    private LocalDateTime fechaPedido;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoPedido estado = EstadoPedido.PENDIENTE;

    @NotNull
    @PositiveOrZero
    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago;

    @Size(max = 255)
    @Column(name = "direccion_envio", length = 255)
    private String direccionEnvio;

    @OneToMany(
        mappedBy = "pedido",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<LineaPedido> lineas = new ArrayList<>();

    /**
     * Anade una linea al pedido manteniendo la consistencia bidireccional.
     * Recalcula el total automaticamente.
     */
    public void anadirLinea(LineaPedido linea) {
        linea.setPedido(this);
        this.lineas.add(linea);
        recalcularTotal();
    }

    /** Recalcula el total como suma de (cantidad * precio_unitario) de cada linea. */
    public void recalcularTotal() {
        this.total = lineas.stream()
                .map(l -> l.getPrecioUnitario().multiply(BigDecimal.valueOf(l.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}