package com.leveluparcade.controller.api;

import com.leveluparcade.llm.LlmException;
import com.leveluparcade.llm.OpenRouterClient;
import com.leveluparcade.llm.OpenRouterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint de diagnostico de la integracion IA.
 *
 * <p>Devuelve si OPENROUTER_API_KEY esta cargado, que modelo se va a
 * usar y -opcionalmente- el resultado de un ping real al modelo.
 *
 * <p>Util en el panel admin para que el operador pueda comprobar de
 * un vistazo por que falla "Generar descripcion" antes de empezar a
 * mirar logs.
 *
 * <ul>
 *   <li>{@code GET /api/llm/estado}        - estado sin tocar la API
 *       (no consume cuota).</li>
 *   <li>{@code GET /api/llm/estado/ping}   - manda un mensaje minimo
 *       al modelo configurado. Si responde, la IA esta operativa.</li>
 * </ul>
 *
 * <p>Acceso restringido a ADMIN.
 */
@RestController
@RequestMapping("/api/llm/estado")
@PreAuthorize("hasRole('ADMIN')")
public class LlmEstadoApiController {

    private static final Logger log = LoggerFactory.getLogger(LlmEstadoApiController.class);

    private final OpenRouterProperties props;
    private final OpenRouterClient client;

    public LlmEstadoApiController(OpenRouterProperties props, OpenRouterClient client) {
        this.props = props;
        this.client = client;
    }

    @GetMapping
    public Map<String, Object> estado() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("configurado", props.estaConfigurado());
        body.put("modelo", props.model());
        body.put("baseUrl", props.baseUrl());
        body.put("timeoutMs", props.timeoutMs());
        body.put("maxTokens", props.maxTokens());
        return body;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("modelo", props.model());

        if (!props.estaConfigurado()) {
            body.put("ok", false);
            body.put("mensaje", "OPENROUTER_API_KEY no esta configurada en el .env.");
            return ResponseEntity.ok(body);
        }

        try {
            String respuesta = client.ping();
            body.put("ok", true);
            body.put("respuesta", respuesta);
            return ResponseEntity.ok(body);
        } catch (LlmException ex) {
            log.warn("Ping IA fallido: {}", ex.getMessage());
            body.put("ok", false);
            body.put("mensaje", ex.getMessage());
            return ResponseEntity.ok(body);
        }
    }
}
