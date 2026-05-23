package com.leveluparcade.auditoria;

/**
 * Evento publicado cuando ocurre una accion auditable.
 *
 * <p>Lo publican los Services con {@code AuditoriaPublisher}, y lo
 * persiste {@code AuditoriaListener}. Es un record inmutable: una vez
 * creado no puede modificarse.
 *
 * @param tipo        tipo de evento (ej: CLIENTE_CREADO)
 * @param entidad     nombre de la entidad afectada ("Cliente", "Proveedor"...)
 * @param entidadId   id de la entidad afectada (puede ser null)
 * @param usuarioId   id del usuario que realiza la accion (puede ser null)
 * @param descripcion descripcion legible del evento
 */
public record AuditoriaEvent(
    TipoEvento tipo,
    String entidad,
    Long entidadId,
    Long usuarioId,
    String descripcion
) {

    /** Helper para eventos sin entidad (ej: logins). */
    public static AuditoriaEvent autenticacion(
            TipoEvento tipo, Long usuarioId, String descripcion) {
        return new AuditoriaEvent(tipo, null, null, usuarioId, descripcion);
    }

    /** Helper para eventos sobre una entidad concreta. */
    public static AuditoriaEvent entidad(
            TipoEvento tipo, String entidad, Long entidadId,
            Long usuarioId, String descripcion) {
        return new AuditoriaEvent(tipo, entidad, entidadId, usuarioId, descripcion);
    }
}