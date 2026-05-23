package com.leveluparcade.exception;

/**
 * Excepcion lanzada cuando una entidad solicitada no existe en BD.
 *
 * <p>Mapeada a HTTP 404 por {@code GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " no encontrado con id: " + id);
    }
}