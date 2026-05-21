package com.leveluparcade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un usuario del sistema.
 *
 * <p>Es la base de autenticación común para todos los tipos de usuario:
 * administradores, empleados y clientes. La diferenciación entre ellos
 * la hace el campo {@link #rol}.</p>
 *
 * <p>Los datos comerciales específicos de los clientes (NIF, dirección, etc.)
 * van en la entidad {@code Cliente}, con relación 1:1 a esta.</p>
 *
 * Tabla asociada: {@code usuarios} (definida en V1__init.sql).
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Size(max = 150, message = "Los apellidos no pueden superar 150 caracteres")
    @Column(name = "apellidos", length = 150)
    private String apellidos;

    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_ultimo_login")
    private LocalDateTime fechaUltimoLogin;

    /**
     * Devuelve el nombre completo concatenado (nombre + apellidos).
     * Útil para mostrar en vistas y logs.
     */
    public String getNombreCompleto() {
        if (apellidos == null || apellidos.isBlank()) {
            return nombre;
        }
        return nombre + " " + apellidos;
    }
}