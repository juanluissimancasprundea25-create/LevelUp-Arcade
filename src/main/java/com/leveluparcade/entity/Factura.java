package com.leveluparcade.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 30)
    private String numeroFactura;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "ruta_pdf", length = 500)
    private String rutaPdf;

    /**
     * Contenido textual del QR (URL de verificación).
     * El nombre de la columna en BD es hash_qr por compatibilidad con V1.
     */
    @Column(name = "hash_qr", length = 255)
    private String contenidoQr;

    @PrePersist
    public void prePersist() {
        if (fechaEmision == null) {
            fechaEmision = LocalDateTime.now();
        }
    }

    /**
     * Calcula el total de la factura a partir del total del pedido asociado.
     * Es un snapshot del pedido en el momento de la emisión.
     */
    @Transient
    public BigDecimal getTotal() {
        return pedido != null ? pedido.getTotal() : BigDecimal.ZERO;
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getRutaPdf() { return rutaPdf; }
    public void setRutaPdf(String rutaPdf) { this.rutaPdf = rutaPdf; }

    public String getContenidoQr() { return contenidoQr; }
    public void setContenidoQr(String contenidoQr) { this.contenidoQr = contenidoQr; }
}