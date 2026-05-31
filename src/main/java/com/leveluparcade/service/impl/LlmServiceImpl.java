package com.leveluparcade.service.impl;

import com.leveluparcade.llm.OpenRouterClient;
import com.leveluparcade.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementacion de {@link LlmService} que construye los prompts y
 * delega la llamada en {@link OpenRouterClient}.
 *
 * <p>Los prompts estan ajustados para devolver respuestas cortas y en
 * castellano, listas para mostrar en el panel admin.
 */
@Service
public class LlmServiceImpl implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmServiceImpl.class);

    private static final String SYSTEM_PROMPT_BASE =
        "Eres un asistente experto en una tienda de videojuegos y merchandising " +
        "llamada LevelUp Arcade. Respondes en castellano neutro, de forma " +
        "concisa y profesional. No usas emojis ni markdown.";

    private final OpenRouterClient client;

    public LlmServiceImpl(OpenRouterClient client) {
        this.client = client;
    }

    @Override
    public String generarDescripcionProducto(String nombreProducto, String categoria) {
        if (nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }

        String userPrompt = "Genera una descripcion comercial de 2 a 4 frases para " +
            "este producto, destacando para que tipo de cliente es interesante. " +
            "No inventes especificaciones tecnicas concretas (procesador, capacidad, " +
            "fecha de salida). Devuelve solo la descripcion, sin titulo ni preambulo." +
            "\n\nNombre: " + nombreProducto +
            (categoria != null && !categoria.isBlank()
                ? "\nCategoria: " + categoria
                : "");

        log.info("Generando descripcion IA para producto: {}", nombreProducto);
        return client.chat(SYSTEM_PROMPT_BASE, userPrompt);
    }

    @Override
    public String sugerirCategoria(String nombreProducto, String descripcion,
                                   List<String> categoriasExistentes) {
        if (nombreProducto == null || nombreProducto.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        if (categoriasExistentes == null || categoriasExistentes.isEmpty()) {
            throw new IllegalArgumentException(
                "Debe haber al menos una categoria existente para sugerir");
        }

        String listado = String.join(", ", categoriasExistentes);

        String userPrompt = "Dada esta lista de categorias existentes en la tienda: " +
            listado + ".\n" +
            "Elige la categoria mas apropiada para el siguiente producto. " +
            "Si ninguna encaja bien, sugiere una nueva categoria corta (1-3 palabras). " +
            "Devuelve UNICAMENTE el nombre de la categoria, sin explicaciones ni puntuacion final." +
            "\n\nProducto: " + nombreProducto +
            (descripcion != null && !descripcion.isBlank()
                ? "\nDescripcion: " + descripcion
                : "");

        log.info("Sugiriendo categoria IA para producto: {}", nombreProducto);
        String respuesta = client.chat(SYSTEM_PROMPT_BASE, userPrompt);

        // Limpieza: la IA a veces devuelve comillas o punto final
        return respuesta.replaceAll("[\"'.]", "").trim();
    }

    /**
     * Prompt de sistema especifico para la atencion al cliente. Es mas
     * abierto que SYSTEM_PROMPT_BASE para permitir conversacion natural,
     * pero pone limites claros: no inventar precios ni stock, no
     * comprometerse en nombre de la empresa, derivar al admin cuando
     * la consulta requiere accion humana.
     */
    private static final String SYSTEM_PROMPT_CLIENTE =
        "Eres el asistente virtual de la tienda online LevelUp Arcade, " +
        "especializada en videojuegos, consolas y merchandising gamer. " +
        "Hablas en castellano neutro, eres amable y vas al grano: " +
        "respuestas de 1 a 4 frases salvo que el cliente pida mas detalle. " +
        "NO inventes precios, stock, fechas de salida, plazos de envio " +
        "ni promociones concretas: si no lo sabes, di que lo confirme " +
        "el equipo de soporte. " +
        "Puedes recomendar generos o tipos de juego segun gustos, " +
        "explicar como funciona la web (catalogo, carrito, pedidos, " +
        "devoluciones, facturas, chat con soporte) y dar consejos " +
        "generales de gaming. " +
        "Si la consulta requiere accion administrativa (cambiar un " +
        "pedido, reembolso, problema con una cuenta), indica al cliente " +
        "que escriba al equipo de soporte desde el chat. " +
        "No uses emojis ni markdown.";

    @Override
    public String responderConsultaCliente(String pregunta) {
        if (pregunta == null || pregunta.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacia");
        }
        // Limite defensivo para no enviar prompts gigantes a la API.
        String preguntaLimpia = pregunta.trim();
        if (preguntaLimpia.length() > 1000) {
            preguntaLimpia = preguntaLimpia.substring(0, 1000);
        }

        log.info("Consulta IA de cliente: {} caracteres", preguntaLimpia.length());
        return client.chat(SYSTEM_PROMPT_CLIENTE, preguntaLimpia);
    }
}