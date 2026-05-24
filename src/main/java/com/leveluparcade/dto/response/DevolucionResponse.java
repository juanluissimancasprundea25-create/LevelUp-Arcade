package com.leveluparcade.dto.response;

import com.leveluparcade.entity.EstadoDevolucion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DevolucionResponse {

    private Long id;
    private Long pedidoId;
    private Long clienteId;
    private LocalDateTime fechaSolicitud;
    private String motivo;
    private EstadoDevolucion estado;
    private BigDecimal importeDevuelto;
    private String observacionesAdmin;
    private List<LineaDevolucionResponse> lineas;

    public DevolucionResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

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

    public List<LineaDevolucionResponse> getLineas() { return lineas; }
    public void setLineas(List<LineaDevolucionResponse> lineas) { this.lineas = lineas; }
}