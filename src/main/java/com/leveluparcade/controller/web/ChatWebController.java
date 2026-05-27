package com.leveluparcade.controller.web;

import com.leveluparcade.dto.request.EnviarMensajeChatRequest;
import com.leveluparcade.dto.response.ConversacionResponse;
import com.leveluparcade.dto.response.MensajeChatResponse;
import com.leveluparcade.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller web para chat interno desde el panel admin.
 *
 * <p>Dos vistas:
 * <ol>
 *   <li>{@code /chat} - listado de conversaciones (clientes que han escrito al buzon admin)</li>
 *   <li>{@code /chat/{clienteUsuarioId}} - historial de una conversacion + envio de respuestas</li>
 * </ol>
 *
 * <p>Llama directamente al ChatService (Opcion A).
 */
@Controller
@RequestMapping("/chat")
@PreAuthorize("hasRole('ADMIN')")
public class ChatWebController {

    private static final String SECCION = "chat";

    private final ChatService chatService;

    public ChatWebController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** Listado de conversaciones. */
    @GetMapping
    public String listar(Model model) {
        List<ConversacionResponse> conversaciones = chatService.listarConversaciones();
        model.addAttribute("conversaciones", conversaciones);
        model.addAttribute("seccionActiva", SECCION);
        return "chat/list";
    }

    /**
     * Conversacion con un cliente concreto.
     * Muestra los mensajes en orden cronologico ascendente (mas recientes abajo).
     */
    @GetMapping("/{clienteUsuarioId}")
    public String conversacion(
            @PathVariable Long clienteUsuarioId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        // Pageable: pedimos por fecha DESC para tener los mas recientes en pagina 0,
        // pero luego invertimos para mostrar los mas viejos arriba y los nuevos abajo
        Pageable pageable = PageRequest.of(
                page, 50, Sort.by(Sort.Direction.DESC, "fechaEnvio"));

        Page<MensajeChatResponse> pagina =
                chatService.historial(clienteUsuarioId, pageable);

        // Invertimos para vista cronologica ascendente (mas viejo arriba)
        List<MensajeChatResponse> mensajes = pagina.getContent().reversed();

        // Sacamos el nombre del cliente desde el listado de conversaciones
        String nombreCliente = chatService.listarConversaciones().stream()
                .filter(c -> clienteUsuarioId.equals(c.getClienteUsuarioId()))
                .map(ConversacionResponse::getClienteNombre)
                .findFirst()
                .orElse("Usuario " + clienteUsuarioId);

        model.addAttribute("clienteUsuarioId", clienteUsuarioId);
        model.addAttribute("clienteNombre", nombreCliente);
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("pagina", pagina);
        model.addAttribute("seccionActiva", SECCION);
        return "chat/conversacion";
    }

    /** Enviar respuesta del admin a un cliente. */
    @PostMapping("/{clienteUsuarioId}")
    public String enviar(
            @PathVariable Long clienteUsuarioId,
            @RequestParam("contenido") String contenido,
            RedirectAttributes ra) {

        if (contenido == null || contenido.isBlank()) {
            ra.addFlashAttribute("flashError", "El mensaje no puede estar vacio.");
            return "redirect:/chat/" + clienteUsuarioId;
        }

        try {
            EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
            req.setDestinatarioUsuarioId(clienteUsuarioId);
            req.setContenido(contenido);
            chatService.enviar(req);
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError",
                    "No se ha podido enviar el mensaje: " + ex.getMessage());
        }
        return "redirect:/chat/" + clienteUsuarioId;
    }

    /** Marca un mensaje como leido (llamado desde JS o como fallback). */
    @PostMapping("/mensajes/{id}/leido")
    public String marcarLeido(
            @PathVariable Long id,
            @RequestParam("clienteUsuarioId") Long clienteUsuarioId) {
        try {
            chatService.marcarLeido(id);
        } catch (Exception ignored) {
            // Si falla, no rompemos la UX
        }
        return "redirect:/chat/" + clienteUsuarioId;
    }
}