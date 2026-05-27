package com.leveluparcade.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para el formulario de registro publico de cliente.
 *
 * <p>Recoge los datos minimos para crear un Usuario (rol CLIENTE) y un Cliente
 * asociado. Los datos opcionales (NIF, telefono, direccion) los puede
 * completar despues en "Mi cuenta".
 */
public class RegistroClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Maximo 100 caracteres")
    private String nombre;

    @Size(max = 150, message = "Maximo 150 caracteres")
    private String apellidos;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email no valido")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, max = 100, message = "La contrasena debe tener entre 8 y 100 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "La contrasena debe contener al menos una letra y un numero"
    )
    private String password;

    @NotBlank(message = "Debes repetir la contrasena")
    private String passwordConfirmacion;

    /** Token de reCAPTCHA v3 (opcional en dev, validado en prod si hay clave). */
    private String recaptchaToken;

    /** El usuario debe aceptar la politica de privacidad explicitamente. */
    private boolean aceptaPrivacidad;

    // --- getters/setters (no usamos Lombok aqui porque no esta en los DTOs request del resto del proyecto) ---

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordConfirmacion() { return passwordConfirmacion; }
    public void setPasswordConfirmacion(String passwordConfirmacion) { this.passwordConfirmacion = passwordConfirmacion; }

    public String getRecaptchaToken() { return recaptchaToken; }
    public void setRecaptchaToken(String recaptchaToken) { this.recaptchaToken = recaptchaToken; }

    public boolean isAceptaPrivacidad() { return aceptaPrivacidad; }
    public void setAceptaPrivacidad(boolean aceptaPrivacidad) { this.aceptaPrivacidad = aceptaPrivacidad; }
}