package com.leveluparcade.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP para la API de OpenRouter (chat completions).
 *
 * <p>Encapsula la comunicacion con {@code POST /chat/completions}.
 * Devuelve el texto plano del primer choice. No mantiene estado entre
 * llamadas, por lo que es seguro como singleton.
 */
@Component
public class OpenRouterClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);

    private final OpenRouterProperties props;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenRouterClient(OpenRouterProperties props) {
        this.props = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Envia un prompt al modelo y devuelve la respuesta como texto.
     *
     * @param systemPrompt instrucciones del rol "system" (puede ser null)
     * @param userPrompt   pregunta o instruccion del usuario
     * @return texto de la respuesta
     * @throws LlmException si la API key no esta configurada, hay error de red,
     *                      el servidor devuelve un error o no se puede parsear
     *                      la respuesta
     */
    public String chat(String systemPrompt, String userPrompt) {

        if (!props.estaConfigurado()) {
            throw new LlmException(
                "OpenRouter no esta configurado. Defina OPENROUTER_API_KEY en el .env");
        }

        String body = buildRequestBody(systemPrompt, userPrompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofMillis(props.timeoutMs()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + props.apiKey())
                .header("HTTP-Referer", "https://leveluparcade.local")
                .header("X-Title", "LevelUp Arcade")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("OpenRouter respondio HTTP {}: {}",
                    response.statusCode(), response.body());
                throw new LlmException(
                    "Error en OpenRouter (HTTP " + response.statusCode() + "). " +
                    "Verifique su API key y el modelo configurado.");
            }

            return extraerContenido(response.body());

        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Fallo la peticion a OpenRouter", e);
            throw new LlmException("No se pudo contactar con OpenRouter: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("model", props.model());
            root.put("max_tokens", props.maxTokens());
            root.put("temperature", 0.7);

            ArrayNode messages = mapper.createArrayNode();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                ObjectNode sys = mapper.createObjectNode();
                sys.put("role", "system");
                sys.put("content", systemPrompt);
                messages.add(sys);
            }
            ObjectNode user = mapper.createObjectNode();
            user.put("role", "user");
            user.put("content", userPrompt);
            messages.add(user);

            root.set("messages", messages);
            return mapper.writeValueAsString(root);

        } catch (Exception e) {
            throw new LlmException("Error construyendo la peticion JSON", e);
        }
    }

    private String extraerContenido(String responseBody) {
        try {
            JsonNode json = mapper.readTree(responseBody);
            JsonNode choices = json.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new LlmException("Respuesta de OpenRouter sin choices");
            }
            String contenido = choices.get(0).path("message").path("content").asText();
            if (contenido == null || contenido.isBlank()) {
                throw new LlmException("Respuesta de OpenRouter sin contenido");
            }
            return contenido.trim();
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("No se pudo parsear la respuesta de OpenRouter", e);
        }
    }
}