package com.leveluparcade.service;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.dto.request.EnviarMensajeChatRequest;
import com.leveluparcade.dto.response.ConversacionResponse;
import com.leveluparcade.dto.response.MensajeChatResponse;
import com.leveluparcade.entity.MensajeChat;
import com.leveluparcade.entity.Rol;
import com.leveluparcade.entity.Usuario;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.MensajeChatRepository;
import com.leveluparcade.repository.UsuarioRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatServiceImpl - tests unitarios")
class ChatServiceImplTest {

    @Mock MensajeChatRepository mensajeRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock SecurityHelper securityHelper;
    @Mock AuditoriaPublisher auditoria;

    @InjectMocks ChatServiceImpl service;

    private Usuario cliente;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        cliente = usuario(10L, "Ana", "Lopez", Rol.CLIENTE);
        admin   = usuario(1L,  "Root", "Admin", Rol.ADMIN);
    }

    // ---------- enviar ----------

    @Test
    @DisplayName("cliente envia sin destinatario -> guarda con destinatario NULL y audita")
    void clienteEnviaAlBuzonAdmin() {
        when(securityHelper.getUsuarioActualId()).thenReturn(10L);
        when(securityHelper.esAdmin()).thenReturn(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(mensajeRepository.save(any(MensajeChat.class))).thenAnswer(inv -> {
            MensajeChat m = inv.getArgument(0);
            m.setId(100L);
            m.setFechaEnvio(LocalDateTime.now());
            return m;
        });

        EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
        req.setDestinatarioUsuarioId(null);
        req.setContenido("Hola, tengo una duda");

        MensajeChatResponse resp = service.enviar(req);

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getDestinatarioId()).isNull();
        assertThat(resp.getRemitenteId()).isEqualTo(10L);
        assertThat(resp.isLeido()).isFalse();

        ArgumentCaptor<AuditoriaEvent> cap = ArgumentCaptor.forClass(AuditoriaEvent.class);
        verify(auditoria).publish(cap.capture());
        assertThat(cap.getValue().tipo()).isEqualTo(TipoEvento.MENSAJE_ENVIADO);
    }

    @Test
    @DisplayName("cliente no puede enviar a otro cliente")
    void clienteNoPuedeEnviarAOtroCliente() {
        Usuario otroCliente = usuario(11L, "Otro", "Cli", Rol.CLIENTE);
        when(securityHelper.getUsuarioActualId()).thenReturn(10L);
        when(securityHelper.esAdmin()).thenReturn(false);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(otroCliente));

        EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
        req.setDestinatarioUsuarioId(11L);
        req.setContenido("eh");

        assertThatThrownBy(() -> service.enviar(req))
            .isInstanceOf(AccessDeniedException.class);
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    @DisplayName("admin sin destinatario -> IllegalArgumentException")
    void adminSinDestinatarioFalla() {
        when(securityHelper.getUsuarioActualId()).thenReturn(1L);
        when(securityHelper.esAdmin()).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));

        EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
        req.setContenido("respondo");

        assertThatThrownBy(() -> service.enviar(req))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("admin responde a cliente concreto -> ok")
    void adminRespondeACliente() {
        when(securityHelper.getUsuarioActualId()).thenReturn(1L);
        when(securityHelper.esAdmin()).thenReturn(true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(mensajeRepository.save(any(MensajeChat.class))).thenAnswer(inv -> {
            MensajeChat m = inv.getArgument(0);
            m.setId(200L);
            m.setFechaEnvio(LocalDateTime.now());
            return m;
        });

        EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
        req.setDestinatarioUsuarioId(10L);
        req.setContenido("Te ayudo");

        MensajeChatResponse resp = service.enviar(req);

        assertThat(resp.getDestinatarioId()).isEqualTo(10L);
        assertThat(resp.getRemitenteId()).isEqualTo(1L);
        verify(auditoria).publish(any(AuditoriaEvent.class));
    }

    @Test
    @DisplayName("sin sesion -> AccessDeniedException")
    void sinSesionFalla() {
        when(securityHelper.getUsuarioActualId()).thenReturn(null);

        EnviarMensajeChatRequest req = new EnviarMensajeChatRequest();
        req.setContenido("hola");

        assertThatThrownBy(() -> service.enviar(req))
            .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- historial ----------

    @Test
    @DisplayName("cliente intenta leer historial de otro -> AccessDenied")
    void clienteNoVeHistorialDeOtro() {
        when(securityHelper.esAdmin()).thenReturn(false);
        when(securityHelper.getUsuarioActualId()).thenReturn(10L);

        assertThatThrownBy(() -> service.historial(99L, PageRequest.of(0, 10)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("admin lee historial de cualquier cliente")
    void adminVeHistorial() {
        when(securityHelper.esAdmin()).thenReturn(true);
        Pageable p = PageRequest.of(0, 10);
        Page<MensajeChat> page = new PageImpl<>(List.of(mensaje(1L, cliente, null, "hola")));
        when(mensajeRepository.historialCliente(10L, p)).thenReturn(page);

        Page<MensajeChatResponse> resp = service.historial(10L, p);

        assertThat(resp.getContent()).hasSize(1);
        assertThat(resp.getContent().get(0).getContenido()).isEqualTo("hola");
    }

    // ---------- marcarLeido ----------

    @Test
    @DisplayName("marcar leido: solo destinatario puede")
    void soloDestinatarioMarcaLeido() {
        MensajeChat m = mensaje(50L, admin, cliente, "respuesta");
        when(mensajeRepository.findById(50L)).thenReturn(Optional.of(m));
        when(securityHelper.getUsuarioActualId()).thenReturn(99L);
        when(securityHelper.esAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.marcarLeido(50L))
            .isInstanceOf(AccessDeniedException.class);
        verify(auditoria, never()).publish(any());
    }

    @Test
    @DisplayName("destinatario marca leido -> persiste y audita")
    void destinatarioMarcaLeido() {
        MensajeChat m = mensaje(50L, admin, cliente, "respuesta");
        when(mensajeRepository.findById(50L)).thenReturn(Optional.of(m));
        when(securityHelper.getUsuarioActualId()).thenReturn(10L);
        when(securityHelper.esAdmin()).thenReturn(false);
        when(mensajeRepository.save(any(MensajeChat.class))).thenAnswer(inv -> inv.getArgument(0));

        MensajeChatResponse resp = service.marcarLeido(50L);

        assertThat(resp.isLeido()).isTrue();
        ArgumentCaptor<AuditoriaEvent> cap = ArgumentCaptor.forClass(AuditoriaEvent.class);
        verify(auditoria).publish(cap.capture());
        assertThat(cap.getValue().tipo()).isEqualTo(TipoEvento.MENSAJE_LEIDO);
    }

    @Test
    @DisplayName("marcar leido un mensaje ya leido es idempotente: no audita de nuevo")
    void marcarLeidoIdempotente() {
        MensajeChat m = mensaje(50L, admin, cliente, "respuesta");
        m.setLeido(true);
        when(mensajeRepository.findById(50L)).thenReturn(Optional.of(m));
        when(securityHelper.getUsuarioActualId()).thenReturn(10L);
        when(securityHelper.esAdmin()).thenReturn(false);

        MensajeChatResponse resp = service.marcarLeido(50L);

        assertThat(resp.isLeido()).isTrue();
        verify(mensajeRepository, never()).save(any());
        verify(auditoria, never()).publish(any());
    }

    @Test
    @DisplayName("buzon admin (destinatario NULL): solo admin puede marcar leido")
    void buzonSoloAdminMarcaLeido() {
        MensajeChat m = mensaje(50L, cliente, null, "duda");
        when(mensajeRepository.findById(50L)).thenReturn(Optional.of(m));
        when(securityHelper.esAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.marcarLeido(50L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("mensaje inexistente -> ResourceNotFound")
    void mensajeInexistente() {
        when(mensajeRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.marcarLeido(404L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- conversaciones ----------

    @Test
    @DisplayName("admin lista conversaciones: una por cliente con mensajes")
    void adminListaConversaciones() {
        when(securityHelper.esAdmin()).thenReturn(true);
        when(mensajeRepository.idsClientesConMensajesAdmin()).thenReturn(List.of(10L));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(cliente));
        when(mensajeRepository.ultimoMensajeDeCliente(eq(10L), any(Pageable.class)))
            .thenReturn(List.of(mensaje(1L, cliente, null, "hola")));
        when(mensajeRepository.countByRemitenteIdAndDestinatarioIsNullAndLeidoFalse(10L))
            .thenReturn(3L);

        List<ConversacionResponse> r = service.listarConversaciones();

        assertThat(r).hasSize(1);
        assertThat(r.get(0).getClienteUsuarioId()).isEqualTo(10L);
        assertThat(r.get(0).getNoLeidos()).isEqualTo(3L);
        assertThat(r.get(0).getUltimoMensaje()).isEqualTo("hola");
    }

    // ---------- helpers ----------

    private Usuario usuario(Long id, String nombre, String apellidos, Rol rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre(nombre);
        u.setApellidos(apellidos);
        u.setRol(rol);
        return u;
    }

    private MensajeChat mensaje(Long id, Usuario rem, Usuario dest, String contenido) {
        MensajeChat m = new MensajeChat();
        m.setId(id);
        m.setRemitente(rem);
        m.setDestinatario(dest);
        m.setContenido(contenido);
        m.setLeido(false);
        m.setFechaEnvio(LocalDateTime.now());
        return m;
    }
}