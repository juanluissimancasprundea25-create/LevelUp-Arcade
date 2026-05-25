package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.DescripcionProductoRequest;
import com.leveluparcade.dto.request.SugerirCategoriaRequest;
import com.leveluparcade.dto.response.CategoriaResponse;
import com.leveluparcade.dto.response.LlmRespuestaResponse;
import com.leveluparcade.llm.OpenRouterProperties;
import com.leveluparcade.service.CategoriaService;
import com.leveluparcade.service.LlmService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST para las funcionalidades de IA del panel admin.
 *
 * <p>Todos los endpoints requieren rol ADMIN. Las llamadas son sincronas
 * y bloquean hasta que OpenRouter responde o se agota el timeout.
 */
@RestController
@RequestMapping("/api/llm")
@PreAuthorize("hasRole('ADMIN')")
public class LlmApiController {

    private final LlmService llmService;
    private final CategoriaService categoriaService;
    private final OpenRouterProperties props;

    public LlmApiController(LlmService llmService,
                            CategoriaService categoriaService,
                            OpenRouterProperties props) {
        this.llmService = llmService;
        this.categoriaService = categoriaService;
        this.props = props;
    }

    @PostMapping("/descripcion-producto")
    public LlmRespuestaResponse generarDescripcion(
            @Valid @RequestBody DescripcionProductoRequest request) {

        String descripcion = llmService.generarDescripcionProducto(
            request.nombreProducto(), request.categoria());

        return new LlmRespuestaResponse(descripcion, props.model());
    }

    @PostMapping("/sugerir-categoria")
    public LlmRespuestaResponse sugerirCategoria(
            @Valid @RequestBody SugerirCategoriaRequest request) {

        List<String> nombresCategorias = categoriaService.listarTodas().stream()
                .map(CategoriaResponse::nombre)
                .toList();

        String sugerencia = llmService.sugerirCategoria(
            request.nombreProducto(),
            request.descripcion(),
            nombresCategorias);

        return new LlmRespuestaResponse(sugerencia, props.model());
    }
}