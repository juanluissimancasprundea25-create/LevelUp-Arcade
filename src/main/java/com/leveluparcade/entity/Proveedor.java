package com.leveluparcade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
 * Entidad que representa un proveedor de productos.
 *
 * <p>Un proveedor es una empresa externa que suministra productos a LevelUp
 * Arcade. Los productos referencian a un proveedor mediante FK. Por tanto,
 * un proveedor con productos asociados no puede borrarse: el {@code
 * ProveedorService} lo valida antes de invocar al repositorio.
 *
 * Tabla asociada: {@code proveedores} (definida en V1__init.sql).
 */
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    @Column(name = "nombre_empresa", nullable = false, length = 200)
    private String nombreEmpresa;

    /**
     * CIF espanol: 1 letra de tipo + 7 digitos + 1 caracter de control
     * (digito o letra, segun la forma juridica).
     * <p>Letras de tipo: A, B, C, D, E, F, G, H, J, N, P, Q, R, S, U, V, W.
     */
    @NotBlank(message = "El CIF es obligatorio")
    @Pattern(
        regexp = "^[ABCDEFGHJNPQRSUVW][0-9]{7}[0-9A-J]$",
        message = "El CIF debe tener formato espanol valido (ej: A12345678)"
    )
    @Size(max = 20)
    @Column(name = "cif", nullable = false, unique = true, length = 20)
    private String cif;

    @Email(message = "El email no tiene un formato valido")
    @Size(max = 150, message = "El email no puede superar 150 caracteres")
    @Column(name = "email_contacto", length = 150)
    private String emailContacto;

    @Pattern(
        regexp = "^[+]?[0-9 ]{6,20}$|^$",
        message = "El telefono no tiene un formato valido"
    )
    @Size(max = 20)
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Size(max = 255)
    @Column(name = "direccion", length = 255)
    private String direccion;

    @Size(max = 100)
    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Pattern(
        regexp = "^[0-9]{4,10}$|^$",
        message = "El codigo postal solo puede contener digitos (4-10)"
    )
    @Size(max = 10)
    @Column(name = "codigo_postal", length = 10)
    private String codigoPostal;

    @Size(max = 100)
    @Column(name = "pais", length = 100)
    @Builder.Default
    private String pais = "Espana";

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;
}