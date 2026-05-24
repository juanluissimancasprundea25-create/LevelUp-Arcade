package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EnviarMensajeChatRequest {

    /** NULL desde un cliente = buzon admin. Obligatorio desde admin. */
    private Long destinatarioUsuarioId;

    @NotBlank(message = "El contenido no puede estar vacio")
    @Size(max = 4000, message = "El contenido no puede superar 4000 caracteres")
    private String contenido;

    public Long getDestinatarioUsuarioId() { return destinatarioUsuarioId; }
    public void setDestinatarioUsuarioId(Long destinatarioUsuarioId) { this.destinatarioUsuarioId = destinatarioUsuarioId; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
}