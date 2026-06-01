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
 *
 * <p>Mejoras de diagnostico:
 * <ul>
 *   <li>Mensajes de error especificos por codigo HTTP (401/402/404/429).</li>
 *   <li>Logea el body crudo cuando la respuesta es 200 pero el contenido
 *       viene vacio (caso tipico de los modelos {@code :free} cuando se
 *       saturan o el modelo ha sido retirado).</li>
 *   <li>Metodo {@link #ping()} para que el panel admin pueda verificar
 *       que la API responde sin tener que crear un producto.</li>
 * </ul>
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
                "OpenRouter no esta configurado. Defina OPENROUTER_API_KEY en el .env "
                + "y reinicie la aplicacion.");
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
                log.error("OpenRouter respondio HTTP {} | modelo={} | body={}",
                    response.statusCode(), props.model(), response.body());
                throw new LlmException(traducirError(response.statusCode(), response.body()));
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

    /**
     * Ping ligero al modelo configurado. Util para que el panel admin
     * pueda verificar el estado de la IA sin necesidad de un producto.
     *
     * @return texto de respuesta (suele ser "OK" o similar)
     */
    public String ping() {
        return chat(
            "Responde unicamente con la palabra OK.",
            "ping");
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

            // Algunos modelos free devuelven 200 con un objeto "error" anidado.
            JsonNode errorNode = json.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                String mensajeError = errorNode.path("message").asText("error desconocido");
                log.error("OpenRouter devolvio 200 con error anidado: {} | body={}",
                    mensajeError, responseBody);
                throw new LlmException(
                    "El modelo '" + props.model() + "' devolvio un error: " + mensajeError);
            }

            JsonNode choices = json.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.error("OpenRouter respondio sin choices | modelo={} | body={}",
                    props.model(), responseBody);
                throw new LlmException(
                    "El modelo '" + props.model() + "' no devolvio resultados. "
                    + "Es posible que el modelo este saturado o haya sido retirado. "
                    + "Prueba con otro en OPENROUTER_MODEL.");
            }
            String contenido = choices.get(0).path("message").path("content").asText();
            if (contenido == null || contenido.isBlank()) {
                log.error("OpenRouter respondio con contenido vacio | modelo={} | body={}",
                    props.model(), responseBody);
                throw new LlmException(
                    "El modelo '" + props.model() + "' devolvio una respuesta vacia. "
                    + "Suele pasar con modelos :free saturados. "
                    + "Cambia OPENROUTER_MODEL a uno disponible (ej. "
                    + "meta-llama/llama-3.3-70b-instruct:free).");
            }
            return contenido.trim();
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("No se pudo parsear la respuesta de OpenRouter", e);
        }
    }

    /**
     * Traduce un codigo HTTP de error de OpenRouter a un mensaje
     * accionable para el usuario final del panel admin.
     */
    private String traducirError(int status, String responseBody) {
        return switch (status) {
            case 401 -> "OpenRouter rechazo la API key (401). Revisa OPENROUTER_API_KEY en el .env.";
            case 402 -> "OpenRouter sin creditos (402). El modelo '" + props.model()
                        + "' requiere saldo. Usa un modelo :free.";
            case 404 -> "OpenRouter no encuentra el modelo '" + props.model() + "' (404). "
                        + "Probablemente ha sido retirado: cambia OPENROUTER_MODEL.";
            case 429 -> "OpenRouter rate-limit (429). El modelo free esta saturado, "
                        + "espera unos segundos o cambia de modelo.";
            default -> "Error en OpenRouter (HTTP " + status + "). "
                       + "Revisa los logs del servidor para mas detalle.";
        };
    }
}
