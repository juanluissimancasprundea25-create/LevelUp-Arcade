package com.leveluparcade.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud de devolucion creada por el cliente o por un admin
 * en nombre del cliente.
 */
public class CrearDevolucionRequest {

    @NotNull(message = "El id del pedido es obligatorio")
    private Long pedidoId;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres")
    private String motivo;

    @NotEmpty(message = "Hay que indicar al menos una linea a devolver")
    @Valid
    private List<LineaDevolucionRequest> lineas;

    public CrearDevolucionRequest() {}

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public List<LineaDevolucionRequest> getLineas() { return lineas; }
    public void setLineas(List<LineaDevolucionRequest> lineas) { this.lineas = lineas; }
}