package com.leveluparcade.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Token de recuperacion de contrasena.
 *
 * <p>Se crea cuando un usuario solicita resetear su clave por email.
 * Caduca tras {@code password-reset.token-expiration-hours} y se marca
 * como {@code usado=true} tras el primer cambio efectivo.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean usado = false;

    public boolean estaExpirado() {
        return LocalDateTime.now().isAfter(fechaExpiracion);
    }

    public boolean esValido() {
        return !usado && !estaExpirado();
    }
}