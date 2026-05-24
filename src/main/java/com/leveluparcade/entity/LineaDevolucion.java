package com.leveluparcade.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lineas_devolucion")
public class LineaDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private Devolucion devolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linea_pedido_id", nullable = false)
    private LineaPedido lineaPedido;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    // === Constructores ===
    public LineaDevolucion() {}

    public LineaDevolucion(Devolucion devolucion, LineaPedido lineaPedido,
                           Integer cantidad, BigDecimal precioUnitario) {
        this.devolucion = devolucion;
        this.lineaPedido = lineaPedido;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    // === Logica de negocio ===
    public BigDecimal getSubtotal() {
        if (precioUnitario == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    // === Getters / Setters ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Devolucion getDevolucion() { return devolucion; }
    public void setDevolucion(Devolucion devolucion) { this.devolucion = devolucion; }

    public LineaPedido getLineaPedido() { return lineaPedido; }
    public void setLineaPedido(LineaPedido lineaPedido) { this.lineaPedido = lineaPedido; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
}