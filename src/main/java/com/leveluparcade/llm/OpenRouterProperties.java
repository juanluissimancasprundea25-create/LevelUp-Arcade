package com.leveluparcade.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuracion del cliente OpenRouter.
 *
 * <p>Se cargan de {@code application.yml} bajo el prefijo {@code openrouter}.
 * Los valores sensibles ({@code apiKey}) provienen de variables de entorno
 * y NO deben subirse al repositorio.
 */
@ConfigurationProperties(prefix = "openrouter")
public record OpenRouterProperties(
    String apiKey,
    String model,
    String baseUrl,
    Integer timeoutMs,
    Integer maxTokens
) {

    /** Indica si la API key esta configurada. */
    public boolean estaConfigurado() {
        return apiKey != null && !apiKey.isBlank();
    }
}