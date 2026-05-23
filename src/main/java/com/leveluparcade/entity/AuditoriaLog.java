package com.leveluparcade.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entrada del log de auditoria.
 *
 * <p>Cada operacion auditable del sistema (escrituras CRUD, intentos de
 * login) genera un registro en esta tabla. Los datos se persisten a
 * traves de {@code AuditoriaListener}, que escucha los eventos
 * publicados por los Services.
 *
 * <p>La FK a usuarios es {@code ON DELETE SET NULL} para preservar el
 * historico aunque el usuario se borre.
 *
 * Tabla asociada: {@code auditoria_log} (definida en V1__init.sql).
 */
@Entity
@Table(name = "auditoria_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AuditoriaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Id del usuario que realizo la accion. Puede ser null si la accion
     * no se asocia a un usuario concreto (ej: login fallido con email
     * inexistente).
     */
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "entidad", length = 50)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @CreationTimestamp
    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;
}