package com.leveluparcade.service.impl;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.dto.request.EnviarMensajeChatRequest;
import com.leveluparcade.dto.response.ConversacionResponse;
import com.leveluparcade.dto.response.MensajeChatResponse;
import com.leveluparcade.entity.MensajeChat;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.MensajeChatRepository;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.ChatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    private final MensajeChatRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityHelper securityHelper;
    private final AuditoriaPublisher auditoria;

    public ChatServiceImpl(
            MensajeChatRepository mensajeRepository,
            UsuarioRepository usuarioRepository,
            SecurityHelper securityHelper,
            AuditoriaPublisher auditoria) {
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.securityHelper = securityHelper;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public MensajeChatResponse enviar(EnviarMensajeChatRequest request) {
        Long remitenteId = securityHelper.getUsuarioActualId();
        if (remitenteId == null) {
            throw new AccessDeniedException("No autenticado");
        }
        Usuario remitente = usuarioRepository.findById(remitenteId)
            .orElseThrow(() -> new ResourceNotFoundException("Remitente no encontrado: " + remitenteId));

        boolean esAdmin = securityHelper.esAdmin();
        Usuario destinatario = resolverDestinatario(request.getDestinatarioUsuarioId(), esAdmin);

        // Si es cliente, no puede dirigirse a otro cliente. Solo NULL (admin) o un admin concreto.
        if (!esAdmin && destinatario != null && esRolCliente(destinatario)) {
            throw new AccessDeniedException("Un cliente no puede enviar mensajes a otro cliente");
        }
        // Admin debe especificar destinatario (no tiene sentido admin -> buzon admin)
        if (esAdmin && destinatario == null) {
            throw new IllegalArgumentException("El destinatario es obligatorio cuando envia un admin");
        }

        MensajeChat m = new MensajeChat();
        m.setRemitente(remitente);
        m.setDestinatario(destinatario);
        m.setContenido(request.getContenido());
        m.setLeido(false);

        MensajeChat guardado = mensajeRepository.save(m);

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.MENSAJE_ENVIADO,
            "MensajeChat",
            guardado.getId(),
            remitenteId,
            "Mensaje " + guardado.getId() + " enviado"
                + (destinatario != null ? " a usuario " + destinatario.getId() : " al buzon admin")
        ));

        return toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversacionResponse> listarConversaciones() {
        if (securityHelper.esAdmin()) {
            return listarConversacionesAdmin();
        }
        return listarConversacionCliente();
    }

    private List<ConversacionResponse> listarConversacionesAdmin() {
        List<Long> clienteIds = mensajeRepository.idsClientesConMensajesAdmin();
        List<ConversacionResponse> resultado = new ArrayList<>();
        for (Long clienteUsuarioId : clienteIds) {
            resultado.add(construirConversacion(clienteUsuarioId, true));
        }
        return resultado;
    }

    private List<ConversacionResponse> listarConversacionCliente() {
        Long usuarioId = securityHelper.getUsuarioActualId();
        if (usuarioId == null) {
            throw new AccessDeniedException("No autenticado");
        }
        ConversacionResponse c = construirConversacion(usuarioId, false);
        // Si nunca ha enviado ni recibido nada, devolvemos una conversacion vacia
        // para que el front muestre el hilo limpio
        return List.of(c);
    }

    private ConversacionResponse construirConversacion(Long clienteUsuarioId, boolean vistaAdmin) {
        ConversacionResponse c = new ConversacionResponse();
        c.setClienteUsuarioId(clienteUsuarioId);

        usuarioRepository.findById(clienteUsuarioId).ifPresent(u ->
            c.setClienteNombre(nombreCompleto(u))
        );

        List<MensajeChat> ultimos = mensajeRepository.ultimoMensajeDeCliente(
            clienteUsuarioId, PageRequest.of(0, 1));
        if (!ultimos.isEmpty()) {
            MensajeChat ultimo = ultimos.get(0);
            c.setUltimoMensaje(ultimo.getContenido());
            c.setFechaUltimoMensaje(ultimo.getFechaEnvio());
        }

        if (vistaAdmin) {
            // Para admin: no leidos del buzon admin que envio este cliente
            c.setNoLeidos(
                mensajeRepository.countByRemitenteIdAndDestinatarioIsNullAndLeidoFalse(clienteUsuarioId)
            );
        } else {
            // Para cliente: respuestas del admin que aun no ha leido
            c.setNoLeidos(
                mensajeRepository.countByDestinatarioIdAndLeidoFalse(clienteUsuarioId)
            );
        }
        return c;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MensajeChatResponse> historial(Long clienteUsuarioId, Pageable pageable) {
        // Cliente solo puede ver el suyo propio
        if (!securityHelper.esAdmin()) {
            Long actual = securityHelper.getUsuarioActualId();
            if (actual == null || !actual.equals(clienteUsuarioId)) {
                throw new AccessDeniedException("No tienes permiso para ver esta conversacion");
            }
        }
        return mensajeRepository.historialCliente(clienteUsuarioId, pageable)
            .map(this::toResponse);
    }

    @Override
    @Transactional
    public MensajeChatResponse marcarLeido(Long mensajeId) {
        MensajeChat m = mensajeRepository.findById(mensajeId)
            .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado: " + mensajeId));

        Long actualId = securityHelper.getUsuarioActualId();
        boolean esAdmin = securityHelper.esAdmin();

        // Solo el destinatario puede marcar leido.
        // Caso buzon admin (destinatario NULL): solo admin.
        // Caso destinatario concreto: solo ese usuario (o admin).
        if (m.getDestinatario() == null) {
            if (!esAdmin) {
                throw new AccessDeniedException("Solo un admin puede marcar leidos del buzon");
            }
        } else {
            boolean esDestinatario = actualId != null && actualId.equals(m.getDestinatario().getId());
            if (!esDestinatario && !esAdmin) {
                throw new AccessDeniedException("Solo el destinatario puede marcar el mensaje como leido");
            }
        }

        if (!m.isLeido()) {
            m.setLeido(true);
            mensajeRepository.save(m);

            auditoria.publish(AuditoriaEvent.entidad(
                TipoEvento.MENSAJE_LEIDO,
                "MensajeChat",
                m.getId(),
                actualId,
                "Mensaje " + m.getId() + " marcado como leido"
            ));
        }
        return toResponse(m);
    }

    // ----------------- helpers -----------------

    private Usuario resolverDestinatario(Long destinatarioId, boolean esAdmin) {
        if (destinatarioId == null) {
            return null; // buzon admin
        }
        return usuarioRepository.findById(destinatarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Destinatario no encontrado: " + destinatarioId));
    }

    private boolean esRolCliente(Usuario u) {
        // Usa toString del enum para no depender del paquete exacto de Rol
        return u.getRol() != null && "CLIENTE".equals(u.getRol().toString());
    }

    private String nombreCompleto(Usuario u) {
        String n = u.getNombre() == null ? "" : u.getNombre();
        String a = u.getApellidos() == null ? "" : u.getApellidos();
        return (n + " " + a).trim();
    }

    private MensajeChatResponse toResponse(MensajeChat m) {
        MensajeChatResponse r = new MensajeChatResponse();
        r.setId(m.getId());
        r.setContenido(m.getContenido());
        r.setLeido(m.isLeido());
        r.setFechaEnvio(m.getFechaEnvio());

        if (m.getRemitente() != null) {
            r.setRemitenteId(m.getRemitente().getId());
            r.setRemitenteNombre(nombreCompleto(m.getRemitente()));
        }
        if (m.getDestinatario() != null) {
            r.setDestinatarioId(m.getDestinatario().getId());
            r.setDestinatarioNombre(nombreCompleto(m.getDestinatario()));
        }
        return r;
    }
}