package com.leveluparcade.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leveluparcade.config.SecurityConfig;
import com.leveluparcade.dto.request.EnviarMensajeChatRequest;
import com.leveluparcade.dto.response.ConversacionResponse;
import com.leveluparcade.dto.response.MensajeChatResponse;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.security.JwtFilter;
import com.leveluparcade.security.JwtService;
import com.leveluparcade.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = ChatApiController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { SecurityConfig.class, JwtFilter.class, JwtService.class }
    )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
@DisplayName("ChatApiController - tests web mvc")
class ChatApiControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean ChatService chatService;

    // ---------- POST /api/chat/mensajes ----------

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("POST mensajes con body valido -> 200")
    void enviarMensajeOk() throws Exception {
        MensajeChatResponse resp = new MensajeChatResponse();
        resp.setId(1L);
        resp.setContenido("hola");
        resp.setFechaEnvio(LocalDateTime.now());
        when(chatService.enviar(any(EnviarMensajeChatRequest.class))).thenReturn(resp);

        EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
        req.setContenido("hola");

        mvc.perform(post("/api/chat/mensajes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.contenido").value("hola"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("POST mensajes con contenido vacio -> 400")
    void enviarMensajeValidacion() throws Exception {
        EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
        req.setContenido("   ");

        mvc.perform(post("/api/chat/mensajes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EMPLEADO")
    @DisplayName("POST mensajes con rol EMPLEADO -> 403")
    void enviarConRolNoAutorizado() throws Exception {
        EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
        req.setContenido("hola");

        mvc.perform(post("/api/chat/mensajes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isForbidden());
    }

    // ---------- GET /api/chat/conversaciones ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET conversaciones (ADMIN) -> 200 con array")
    void listarConversacionesAdmin() throws Exception {
        ConversacionResponse c = new ConversacionResponse();
        c.setClienteUsuarioId(10L);
        c.setClienteNombre("Ana Lopez");
        c.setNoLeidos(2L);
        when(chatService.listarConversaciones()).thenReturn(List.of(c));

        mvc.perform(get("/api/chat/conversaciones"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].clienteUsuarioId").value(10))
            .andExpect(jsonPath("$[0].noLeidos").value(2));
    }

    // ---------- GET /api/chat/conversaciones/{id}/mensajes ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET historial (ADMIN) -> 200 con Page")
    void historialAdmin() throws Exception {
        MensajeChatResponse m = new MensajeChatResponse();
        m.setId(1L);
        m.setContenido("hola");
        Page<MensajeChatResponse> page = new PageImpl<>(List.of(m));
        when(chatService.historial(eq(10L), any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/api/chat/conversaciones/10/mensajes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].contenido").value("hola"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("GET historial de otro cliente -> 403 cuando el service deniega")
    void historialClienteDenegado() throws Exception {
        when(chatService.historial(eq(99L), any(Pageable.class)))
            .thenThrow(new AccessDeniedException("no"));

        mvc.perform(get("/api/chat/conversaciones/99/mensajes"))
            .andExpect(status().isForbidden());
    }

    // ---------- PATCH /api/chat/mensajes/{id}/leido ----------

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("PATCH leido -> 200")
    void marcarLeidoOk() throws Exception {
        MensajeChatResponse resp = new MensajeChatResponse();
        resp.setId(5L);
        resp.setLeido(true);
        when(chatService.marcarLeido(5L)).thenReturn(resp);

        mvc.perform(patch("/api/chat/mensajes/5/leido"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.leido").value(true));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("PATCH leido sobre mensaje inexistente -> 404")
    void marcarLeidoNoEncontrado() throws Exception {
        when(chatService.marcarLeido(404L))
            .thenThrow(new ResourceNotFoundException("no existe"));

        mvc.perform(patch("/api/chat/mensajes/404/leido"))
            .andExpect(status().isNotFound());
    }
}