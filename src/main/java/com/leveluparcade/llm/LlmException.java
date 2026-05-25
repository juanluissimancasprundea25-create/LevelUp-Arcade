package com.leveluparcade.llm;

/**
 * Excepcion para errores de comunicacion o configuracion del LLM.
 *
 * <p>El {@code GlobalExceptionHandler} la mapea a HTTP 503 Service Unavailable
 * porque el LLM es un servicio externo del que dependemos.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}