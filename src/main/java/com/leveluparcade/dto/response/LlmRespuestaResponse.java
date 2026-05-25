package com.leveluparcade.dto.response;

/**
 * Respuesta generica de un endpoint de IA.
 *
 * <p>{@code modelo} se incluye para que el cliente pueda mostrar de
 * forma transparente con que modelo se genero el resultado.
 */
public record LlmRespuestaResponse(
    String resultado,
    String modelo
) {}