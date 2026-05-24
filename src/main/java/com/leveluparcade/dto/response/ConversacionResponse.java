package com.leveluparcade.dto.response;

import java.time.LocalDateTime;

public class ConversacionResponse {
    private Long clienteUsuarioId;
    private String clienteNombre;
    private String ultimoMensaje;
    private LocalDateTime fechaUltimoMensaje;
    private long noLeidos;

    public Long getClienteUsuarioId() { return clienteUsuarioId; }
    public void setClienteUsuarioId(Long clienteUsuarioId) { this.clienteUsuarioId = clienteUsuarioId; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getUltimoMensaje() { return ultimoMensaje; }
    public void setUltimoMensaje(String ultimoMensaje) { this.ultimoMensaje = ultimoMensaje; }

    public LocalDateTime getFechaUltimoMensaje() { return fechaUltimoMensaje; }
    public void setFechaUltimoMensaje(LocalDateTime fechaUltimoMensaje) { this.fechaUltimoMensaje = fechaUltimoMensaje; }

    public long getNoLeidos() { return noLeidos; }
    public void setNoLeidos(long noLeidos) { this.noLeidos = noLeidos; }
}