package com.leveluparcade.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
 * Entidad que representa los datos comerciales de un cliente.
 *
 * <p>Cada cliente tiene una relación 1:1 con un {@link Usuario}, que contiene
 * la información de autenticación (email, contraseña, rol). Esta entidad
 * añade la información comercial: NIF, dirección de envío, teléfono, etc.</p>
 *
 * <p>Solo los usuarios con rol {@link Rol#CLIENTE} deberían tener un registro
 * en esta tabla.</p>
 *
 * Tabla asociada: {@code clientes} (definida en V1__init.sql).
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "usuario")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Usuario asociado a este cliente (relación 1:1 obligatoria).
     * Si se borra el usuario, se borra también el cliente (ON DELETE CASCADE).
     */
    @NotNull(message = "El cliente debe estar asociado a un usuario")
    @Valid
    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Pattern(
        regexp = "^[0-9]{8}[A-Za-z]$|^[XYZ][0-9]{7}[A-Za-z]$|^$",
        message = "El NIF debe tener formato válido (8 dígitos + letra, o letra + 7 dígitos + letra)"
    )
    @Size(max = 20, message = "El NIF no puede superar 20 caracteres")
    @Column(name = "nif", unique = true, length = 20)
    private String nif;

    @Pattern(
        regexp = "^[+]?[0-9 ]{6,20}$|^$",
        message = "El teléfono no tiene un formato válido"
    )
    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    @Column(name = "direccion", length = 255)
    private String direccion;

    @Size(max = 100, message = "La ciudad no puede superar 100 caracteres")
    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Pattern(
        regexp = "^[0-9]{4,10}$|^$",
        message = "El código postal solo puede contener dígitos (entre 4 y 10)"
    )
    @Size(max = 10, message = "El código postal no puede superar 10 caracteres")
    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Size(max = 100, message = "El país no puede superar 100 caracteres")
    @Column(name = "pais", length = 100)
    @Builder.Default
    private String pais = "España";

    @CreationTimestamp
    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;
}