package com.leveluparcade.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devoluciones")
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDevolucion estado;

    @Column(name = "importe_devuelto", precision = 10, scale = 2)
    private BigDecimal importeDevuelto;

    @Column(name = "observaciones_admin", columnDefinition = "TEXT")
    private String observacionesAdmin;

    @OneToMany(mappedBy = "devolucion",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    private List<LineaDevolucion> lineas = new ArrayList<>();

    // === Constructores ===
    public Devolucion() {}

    @PrePersist
    protected void onCreate() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoDevolucion.SOLICITADA;
        }
    }

    // === Helpers para mantener la relacion bidireccional ===
    public void addLinea(LineaDevolucion linea) {
        lineas.add(linea);
        linea.setDevolucion(this);
    }

    public void removeLinea(LineaDevolucion linea) {
        lineas.remove(linea);
        linea.setDevolucion(null);
    }

    /**
     * Calcula el importe total a devolver a partir de las lineas.
     * No modifica importeDevuelto: lo devuelve para que el service decida.
     */
    public BigDecimal calcularImporte() {
        return lineas.stream()
            .map(LineaDevolucion::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // === Getters / Setters ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public EstadoDevolucion getEstado() { return estado; }
    public void setEstado(EstadoDevolucion estado) { this.estado = estado; }

    public BigDecimal getImporteDevuelto() { return importeDevuelto; }
    public void setImporteDevuelto(BigDecimal importeDevuelto) { this.importeDevuelto = importeDevuelto; }

    public String getObservacionesAdmin() { return observacionesAdmin; }
    public void setObservacionesAdmin(String observacionesAdmin) { this.observacionesAdmin = observacionesAdmin; }

    public List<LineaDevolucion> getLineas() { return lineas; }
    public void setLineas(List<LineaDevolucion> lineas) { this.lineas = lineas; }
}