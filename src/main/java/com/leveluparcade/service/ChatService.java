package com.leveluparcade.service;

import com.leveluparcade.dto.request.EnviarMensajeChatRequest;
import com.leveluparcade.dto.response.ConversacionResponse;
import com.leveluparcade.dto.response.MensajeChatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatService {

    MensajeChatResponse enviar(EnviarMensajeChatRequest request);

    List<ConversacionResponse> listarConversaciones();

    Page<MensajeChatResponse> historial(Long clienteUsuarioId, Pageable pageable);

    MensajeChatResponse marcarLeido(Long mensajeId);
}