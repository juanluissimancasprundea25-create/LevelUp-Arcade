package com.leveluparcade.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FacturaResponse {

    private Long id;
    private String numeroFactura;
    private LocalDateTime fechaEmision;
    private Long pedidoId;
    private Long clienteId;
    private String clienteNombre;
    private BigDecimal total;
    private String urlVerificacion;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getUrlVerificacion() { return urlVerificacion; }
    public void setUrlVerificacion(String urlVerificacion) { this.urlVerificacion = urlVerificacion; }
}