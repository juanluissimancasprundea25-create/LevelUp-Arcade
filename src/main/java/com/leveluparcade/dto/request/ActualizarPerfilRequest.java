package com.leveluparcade.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos editables del perfil del cliente desde la tienda.
 *
 * <p>NO incluye email (es el login) ni nif (legalmente dificil de cambiar).
 * Para cambios sensibles, el cliente debe contactar por chat con un admin.
 */
public class ActualizarPerfilRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Maximo 100 caracteres")
    private String nombre;

    @Size(max = 150, message = "Maximo 150 caracteres")
    private String apellidos;

    @Size(max = 20, message = "Maximo 20 caracteres")
    private String telefono;

    @Size(max = 255, message = "Maximo 255 caracteres")
    private String direccion;

    @Size(max = 100, message = "Maximo 100 caracteres")
    private String ciudad;

    @Size(max = 10, message = "Maximo 10 caracteres")
    private String codigoPostal;

    @Size(max = 100, message = "Maximo 100 caracteres")
    private String pais;

    public ActualizarPerfilRequest() {}

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public String getCodigoPostal() { return codigoPostal; }
    public void setCodigoPostal(String codigoPostal) { this.codigoPostal = codigoPostal; }

    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
}
