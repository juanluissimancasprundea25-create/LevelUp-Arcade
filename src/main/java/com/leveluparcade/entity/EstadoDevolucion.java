package com.leveluparcade.entity;

/**
 * Estados del ciclo de vida de una devolucion.
 *
 * Flujo:
 *   SOLICITADA -> APROBADA -> COMPLETADA
 *   SOLICITADA -> RECHAZADA (fin)
 *
 * Coincide con el CHECK constraint de la tabla devoluciones en V1.
 */
public enum EstadoDevolucion {
    SOLICITADA,
    APROBADA,
    RECHAZADA,
    COMPLETADA
}