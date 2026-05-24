package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * El admin rechaza una devolucion: motivo del rechazo obligatorio
 * para que el cliente sepa por que.
 */
public class RechazarDevolucionRequest {

    @NotBlank(message = "El motivo de rechazo es obligatorio")
    @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres")
    private String motivoRechazo;

    public RechazarDevolucionRequest() {}

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }
}