package com.leveluparcade.controller.web;

import com.leveluparcade.dto.request.EnviarMensajeChatRequest;
import com.leveluparcade.dto.response.MensajeChatResponse;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat del CLIENTE con el equipo de soporte (admin).
 *
 * <p>Tres endpoints bajo {@code /cuenta/chat/**}, protegidos por la cadena 3
 * de SecurityConfig (ROLE_CLIENTE, CSRF desactivado en esa cadena):
 * <ul>
 *   <li>{@code GET /cuenta/chat} - vista de la conversacion + form de envio.</li>
 *   <li>{@code POST /cuenta/chat/enviar} - envia un mensaje al buzon admin
 *       (destinatarioUsuarioId = null).</li>
 *   <li>{@code GET /cuenta/chat/mensajes} - devuelve JSON con el historial.
 *       Usado por el polling JS de la pagina para refrescar sin recargar.</li>
 * </ul>
 *
 * <p>NO duplica logica: reutiliza {@link ChatService} que ya:
 * <ul>
 *   <li>Resuelve el remitente via SecurityHelper.</li>
 *   <li>Impide que un cliente envie a otro cliente.</li>
 *   <li>Impide que un cliente vea el historial de otro.</li>
 *   <li>Audita el envio.</li>
 * </ul>
 */
@Controller
@RequestMapping("/cuenta/chat")
public class ChatClienteWebController {

    /** Tamano de pagina del historial: ajustado para ver muchos mensajes de golpe. */
    private static final int TAMANO_HISTORIAL = 100;

    private final ChatService chatService;
    private final SecurityHelper securityHelper;

    public ChatClienteWebController(ChatService chatService,
                                    SecurityHelper securityHelper) {
        this.chatService = chatService;
        this.securityHelper = securityHelper;
    }

    /** Pagina del chat: lista de mensajes + form de envio. */
    @GetMapping
    public String chat(Model model) {
        Long usuarioId = securityHelper.getUsuarioActualId();
        List<MensajeChatResponse> mensajes = cargarMensajes(usuarioId);

        // Marca como leidos los mensajes que el admin nos haya enviado.
        marcarLeidosDelAdmin(mensajes, usuarioId);

        model.addAttribute("mensajes", mensajes);
        model.addAttribute("usuarioId", usuarioId);
        model.addAttribute("seccionCuenta", "chat");
        return "tienda/cuenta/chat";
    }

    /** Envia un mensaje al buzon admin (destinatarioUsuarioId = null). */
    @PostMapping("/enviar")
    public String enviar(@RequestParam("contenido") String contenido,
                         RedirectAttributes flash) {
        if (contenido == null || contenido.isBlank()) {
            flash.addFlashAttribute("error", "El mensaje no puede estar vacio.");
            return "redirect:/cuenta/chat";
        }
        EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
        req.setDestinatarioUsuarioId(null); // null = buzon admin
        req.setContenido(contenido.trim());
        try {
            chatService.enviar(req);
        } catch (RuntimeException ex) {
            flash.addFlashAttribute("error", "No se ha podido enviar el mensaje.");
        }
        return "redirect:/cuenta/chat";
    }

    /**
     * Endpoint JSON para el polling del navegador. Devuelve el historial
     * del cliente actual en orden cronologico ascendente.
     *
     * <p>NO requiere JWT porque vive en la cadena 3 (sesion del cliente
     * logueado). El propio ChatService valida que solo se vea el propio
     * historial.
     */
    @GetMapping("/mensajes")
    @ResponseBody
    public List<MensajeChatResponse> mensajesJson() {
        Long usuarioId = securityHelper.getUsuarioActualId();
        return cargarMensajes(usuarioId);
    }

    // ---------- helpers ----------

    /** Trae el historial en orden cronologico ascendente (viejos arriba). */
    private List<MensajeChatResponse> cargarMensajes(Long usuarioId) {
        if (usuarioId == null) {
            return new ArrayList<>();
        }
        // Pedimos DESC para tener los mas recientes en pagina 0 si hubiera muchos,
        // pero invertimos para que la vista los muestre cronologicamente.
        Page<MensajeChatResponse> pagina = chatService.historial(
                usuarioId,
                PageRequest.of(0, TAMANO_HISTORIAL,
                        Sort.by(Sort.Direction.DESC, "fechaEnvio")));
        List<MensajeChatResponse> lista = new ArrayList<>(pagina.getContent());
        java.util.Collections.reverse(lista);
        return lista;
    }

    /**
     * Marca como leidos los mensajes cuyo destinatario sea el usuario actual
     * (es decir, los que el admin le ha enviado al cliente). Silencioso ante
     * cualquier fallo: si uno no se puede marcar, seguimos.
     */
    private void marcarLeidosDelAdmin(List<MensajeChatResponse> mensajes, Long usuarioId) {
        if (usuarioId == null) return;
        for (MensajeChatResponse m : mensajes) {
            if (!m.isLeido()
                    && m.getDestinatarioId() != null
                    && usuarioId.equals(m.getDestinatarioId())) {
                try {
                    chatService.marcarLeido(m.getId());
                } catch (Exception ignored) {
                    // no critico: si falla, lo dejamos sin marcar
                }
            }
        }
    }
}
