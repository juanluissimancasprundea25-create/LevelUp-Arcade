package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.EnviarMensajeChatRequest;
import com.leveluparcade.dto.response.ConversacionResponse;
import com.leveluparcade.dto.response.MensajeChatResponse;
import com.leveluparcade.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;

    public ChatApiController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/mensajes")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<MensajeChatResponse> enviar(
            @Valid @RequestBody EnviarMensajeChatRequest request) {
        return ResponseEntity.ok(chatService.enviar(request));
    }

    @GetMapping("/conversaciones")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<List<ConversacionResponse>> listarConversaciones() {
        return ResponseEntity.ok(chatService.listarConversaciones());
    }

    @GetMapping("/conversaciones/{clienteUsuarioId}/mensajes")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<Page<MensajeChatResponse>> historial(
            @PathVariable Long clienteUsuarioId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(chatService.historial(clienteUsuarioId, pageable));
    }

    @PatchMapping("/mensajes/{id}/leido")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<MensajeChatResponse> marcarLeido(@PathVariable Long id) {
        return ResponseEntity.ok(chatService.marcarLeido(id));
    }
}