package com.leveluparcade.repository;

import com.leveluparcade.entity.MensajeChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MensajeChatRepository extends JpaRepository<MensajeChat, Long> {

    /**
     * Historial entre un cliente concreto y los admins (incluye buzon comun).
     * Devuelve mensajes donde:
     *  - remitente = clienteUsuarioId (los que envia el cliente), o
     *  - destinatario = clienteUsuarioId (respuestas dirigidas al cliente)
     */
    @Query("""
        SELECT m FROM MensajeChat m
        WHERE m.remitente.id = :clienteUsuarioId
           OR m.destinatario.id = :clienteUsuarioId
    """)
    Page<MensajeChat> historialCliente(
        @Param("clienteUsuarioId") Long clienteUsuarioId,
        Pageable pageable
    );

    /**
     * Lista de ids de usuario que han participado como remitente en mensajes
     * dirigidos al buzon admin (destinatario NULL) o como destinatario desde un admin.
     * Usado para listar conversaciones desde la vista admin.
     */
    @Query("""
        SELECT DISTINCT m.remitente.id FROM MensajeChat m
        WHERE m.destinatario IS NULL
           AND m.remitente.rol = com.leveluparcade.entity.Rol.CLIENTE
    """)
    List<Long> idsClientesConMensajesAdmin();

    /** Ultimo mensaje del hilo de un cliente (para preview). */
    @Query("""
        SELECT m FROM MensajeChat m
        WHERE m.remitente.id = :clienteUsuarioId
           OR m.destinatario.id = :clienteUsuarioId
        ORDER BY m.fechaEnvio DESC
    """)
    List<MensajeChat> ultimoMensajeDeCliente(
        @Param("clienteUsuarioId") Long clienteUsuarioId,
        Pageable pageable
    );

    /** No leidos dirigidos a un usuario concreto. */
    long countByDestinatarioIdAndLeidoFalse(Long destinatarioId);

    /** No leidos en el buzon admin (destinatario NULL). */
    long countByDestinatarioIsNullAndLeidoFalse();

    /** No leidos en el buzon admin enviados por un cliente concreto. */
    long countByRemitenteIdAndDestinatarioIsNullAndLeidoFalse(Long remitenteId);

    /**
     * Marca como leidos todos los mensajes que el cliente indicado ha
     * enviado al buzon admin (destinatario NULL) y que aun no se hayan
     * leido. Usado cuando un admin abre la conversacion con ese cliente.
     *
     * @return numero de filas actualizadas.
     */
    @Modifying
    @Query("""
        UPDATE MensajeChat m
           SET m.leido = true
         WHERE m.remitente.id = :remitenteId
           AND m.destinatario IS NULL
           AND m.leido = false
    """)
    int marcarLeidosDelBuzonParaCliente(@Param("remitenteId") Long remitenteId);
}