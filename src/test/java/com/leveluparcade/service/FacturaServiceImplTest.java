package com.leveluparcade.service;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.config.FacturasProperties;
import com.leveluparcade.dto.request.EmitirFacturaRequest;
import com.leveluparcade.dto.response.FacturaResponse;
import com.leveluparcade.entity.*;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.FacturaRepository;
import com.leveluparcade.repository.PedidoRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.impl.FacturaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Disabled;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Disabled("Pendiente de actualizar tras retirar el sistema de verificacion por QR")
@ExtendWith(MockitoExtension.class)
@DisplayName("FacturaServiceImpl - tests unitarios")
class FacturaServiceImplTest {

    @Mock private FacturaRepository facturaRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private NumeradorFacturas numerador;
    @Mock private PdfFacturaService pdfService;
    @Mock private FacturasProperties props;
    @Mock private SecurityHelper securityHelper;
    @Mock private AuditoriaPublisher auditoriaPublisher;

    @InjectMocks private FacturaServiceImpl service;

    private Pedido pedidoPagado;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("c@test.local");
        usuario.setNombre("Cliente Test");

        cliente = new Cliente();
        cliente.setId(5L);
        cliente.setUsuario(usuario);

        pedidoPagado = new Pedido();
        pedidoPagado.setId(1L);
        pedidoPagado.setCliente(cliente);
        pedidoPagado.setEstado(EstadoPedido.PAGADO);
        pedidoPagado.setTotal(new BigDecimal("100.00"));
    }

    // ============================================
    // emitirFactura
    // ============================================

    @Test
    @DisplayName("emitirFactura: exito con pedido PAGADO genera numero y publica evento")
    void emitir_exito() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoPagado));
        when(facturaRepository.findByPedidoId(1L)).thenReturn(Optional.empty());
        when(numerador.generarSiguiente()).thenReturn("FAC-2026-00001");
        when(props.getUrlVerificacionBase()).thenReturn("http://test/verificar");
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            f.setId(77L);
            return f;
        });

        EmitirFacturaRequest req = new EmitirFacturaRequest();
        req.setPedidoId(1L);

        FacturaResponse resp = service.emitirFactura(req);

        assertThat(resp.getNumeroFactura()).isEqualTo("FAC-2026-00001");
        assertThat(resp.getPedidoId()).isEqualTo(1L);
        assertThat(resp.getUrlVerificacion()).isEqualTo("http://test/verificar/FAC-2026-00001");
        verify(auditoriaPublisher).publish(any(AuditoriaEvent.class));
    }

    @Test
    @DisplayName("emitirFactura: idempotente - mismo pedido devuelve factura existente")
    void emitir_idempotente() {
        Factura existente = new Factura();
        existente.setId(99L);
        existente.setPedido(pedidoPagado);
        existente.setNumeroFactura("FAC-2026-00042");
        existente.setFechaEmision(LocalDateTime.now());
        existente.setContenidoQr("http://test/verificar/FAC-2026-00042");

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoPagado));
        when(facturaRepository.findByPedidoId(1L)).thenReturn(Optional.of(existente));

        EmitirFacturaRequest req = new EmitirFacturaRequest();
        req.setPedidoId(1L);

        FacturaResponse resp = service.emitirFactura(req);

        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getNumeroFactura()).isEqualTo("FAC-2026-00042");
        verify(numerador, never()).generarSiguiente();
        verify(facturaRepository, never()).save(any());
        verify(auditoriaPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("emitirFactura: falla si pedido esta PENDIENTE")
    void emitir_falla_pedidoPendiente() {
        pedidoPagado.setEstado(EstadoPedido.PENDIENTE);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoPagado));
        when(facturaRepository.findByPedidoId(1L)).thenReturn(Optional.empty());

        EmitirFacturaRequest req = new EmitirFacturaRequest();
        req.setPedidoId(1L);

        assertThatThrownBy(() -> service.emitirFactura(req))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("PENDIENTE");

        verify(facturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("emitirFactura: falla si pedido esta CANCELADO")
    void emitir_falla_pedidoCancelado() {
        pedidoPagado.setEstado(EstadoPedido.CANCELADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoPagado));
        when(facturaRepository.findByPedidoId(1L)).thenReturn(Optional.empty());

        EmitirFacturaRequest req = new EmitirFacturaRequest();
        req.setPedidoId(1L);

        assertThatThrownBy(() -> service.emitirFactura(req))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("emitirFactura: pedido inexistente -> ResourceNotFoundException")
    void emitir_falla_pedidoInexistente() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        EmitirFacturaRequest req = new EmitirFacturaRequest();
        req.setPedidoId(999L);

        assertThatThrownBy(() -> service.emitirFactura(req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============================================
    // obtenerPorId
    // ============================================

    @Test
    @DisplayName("obtenerPorId: admin accede a cualquier factura")
    void obtener_admin_ok() {
        Factura f = facturaConPedido();
        when(facturaRepository.findById(77L)).thenReturn(Optional.of(f));
        when(securityHelper.esAdmin()).thenReturn(true);

        FacturaResponse resp = service.obtenerPorId(77L);

        assertThat(resp.getId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("obtenerPorId: cliente dueno accede a su factura")
    void obtener_clienteDueno_ok() {
        Factura f = facturaConPedido();
        when(facturaRepository.findById(77L)).thenReturn(Optional.of(f));
        when(securityHelper.esAdmin()).thenReturn(false);
        when(securityHelper.getClienteActualId()).thenReturn(5L);

        FacturaResponse resp = service.obtenerPorId(77L);

        assertThat(resp.getClienteId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("obtenerPorId: cliente NO dueno -> AccessDenied")
    void obtener_clienteNoDueno_denegado() {
        Factura f = facturaConPedido();
        when(facturaRepository.findById(77L)).thenReturn(Optional.of(f));
        when(securityHelper.esAdmin()).thenReturn(false);
        when(securityHelper.getClienteActualId()).thenReturn(999L);

        assertThatThrownBy(() -> service.obtenerPorId(77L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("obtenerPorId: factura inexistente -> ResourceNotFoundException")
    void obtener_falla_inexistente() {
        when(facturaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============================================
    // verificar (endpoint publico)
    // ============================================

    @Test
    @DisplayName("verificar: devuelve factura por numero sin chequeos de acceso")
    void verificar_ok() {
        Factura f = facturaConPedido();
        when(facturaRepository.findByNumeroFactura("FAC-2026-00001")).thenReturn(Optional.of(f));

        FacturaResponse resp = service.verificar("FAC-2026-00001");

        assertThat(resp.getNumeroFactura()).isEqualTo("FAC-2026-00001");
        // No se consulta securityHelper en este flujo
        verify(securityHelper, never()).esAdmin();
    }

    @Test
    @DisplayName("verificar: numero inexistente -> ResourceNotFoundException")
    void verificar_falla_inexistente() {
        when(facturaRepository.findByNumeroFactura(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verificar("FAC-2026-99999"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============================================
    // descargarPdf
    // ============================================

    @Test
    @DisplayName("descargarPdf: genera PDF y publica evento FACTURA_DESCARGADA")
    void descargar_exito() {
        Factura f = facturaConPedido();
        when(facturaRepository.findById(77L)).thenReturn(Optional.of(f));
        when(securityHelper.esAdmin()).thenReturn(true);
        when(pdfService.generar(f)).thenReturn(new byte[]{1, 2, 3, 4});

        byte[] pdf = service.descargarPdf(77L);

        assertThat(pdf).hasSize(4);
        verify(auditoriaPublisher).publish(any(AuditoriaEvent.class));
    }

    // ============================================
    // Helpers
    // ============================================

    private Factura facturaConPedido() {
        Factura f = new Factura();
        f.setId(77L);
        f.setNumeroFactura("FAC-2026-00001");
        f.setFechaEmision(LocalDateTime.now());
        f.setPedido(pedidoPagado);
        f.setContenidoQr("http://test/verificar/FAC-2026-00001");
        return f;
    }
}