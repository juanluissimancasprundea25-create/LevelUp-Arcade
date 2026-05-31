package com.leveluparcade.controller.web;

import com.leveluparcade.llm.LlmException;
import com.leveluparcade.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * Pantalla y endpoint del asistente IA del CLIENTE.
 *
 * <p>Bajo {@code /cuenta/asistente}, protegido por la cadena 3 de
 * SecurityConfig (ROLE_CLIENTE, CSRF desactivado en esa cadena).
 *
 * <ul>
 *   <li>{@code GET /cuenta/asistente} - vista del chat IA.</li>
 *   <li>{@code POST /cuenta/asistente/preguntar} - acepta JSON
 *       {@code {"pregunta":"..."}} y devuelve {@code {"respuesta":"..."}}.
 *       El historial se conserva en sessionStorage del navegador, no
 *       en BD, para no llenar tablas con conversaciones efimeras.</li>
 * </ul>
 *
 * <p>Reutiliza {@link LlmService#responderConsultaCliente(String)}, que
 * a su vez usa el mismo {@code OpenRouterClient} que la IA del admin.
 * No se necesita ninguna config nueva: si OPENROUTER_API_KEY ya esta
 * en el .env funciona directamente.
 */
@Controller
@RequestMapping("/cuenta/asistente")
@PreAuthorize("hasRole('CLIENTE')")
public class AsistenteIaWebController {

    private static final Logger log = LoggerFactory.getLogger(AsistenteIaWebController.class);

    private final LlmService llmService;

    public AsistenteIaWebController(LlmService llmService) {
        this.llmService = llmService;
    }

    @GetMapping
    public String pantalla(Model model) {
        model.addAttribute("seccionCuenta", "asistente");
        return "tienda/cuenta/asistente";
    }

    /**
     * Pregunta-respuesta sincrona. JSON in, JSON out.
     * Espera {@code {"pregunta": "texto"}} y devuelve uno de:
     * <ul>
     *   <li>200 {@code {"respuesta": "..."}}</li>
     *   <li>400 {@code {"error": "..."}}   - pregunta invalida</li>
     *   <li>503 {@code {"error": "..."}}   - IA no disponible / mal config</li>
     * </ul>
     */
    @PostMapping("/preguntar")
    @ResponseBody
    public ResponseEntity<Map<String, String>> preguntar(
            @RequestBody Map<String, String> body) {

        String pregunta = body != null ? body.get("pregunta") : null;
        if (pregunta == null || pregunta.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La pregunta no puede estar vacia."));
        }

        try {
            String respuesta = llmService.responderConsultaCliente(pregunta);
            return ResponseEntity.ok(Map.of("respuesta", respuesta));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ex.getMessage()));
        } catch (LlmException ex) {
            log.warn("Asistente IA no disponible: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error",
                            "El asistente IA no esta disponible ahora mismo. "
                            + "Intentalo de nuevo en unos minutos o escribe al "
                            + "equipo de soporte."));
        } catch (Exception ex) {
            log.error("Error inesperado en asistente IA", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error",
                            "Ha ocurrido un error procesando tu pregunta."));
        }
    }
}