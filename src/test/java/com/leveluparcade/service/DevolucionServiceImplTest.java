package com.leveluparcade.service;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.config.DevolucionesProperties;
import com.leveluparcade.dto.request.CrearDevolucionRequest;
import com.leveluparcade.dto.request.LineaDevolucionRequest;
import com.leveluparcade.dto.request.RechazarDevolucionRequest;
import com.leveluparcade.dto.response.DevolucionResponse;
import com.leveluparcade.entity.*;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.DevolucionRepository;
import com.leveluparcade.repository.PedidoRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.impl.DevolucionServiceImpl;
import com.leveluparcade.util.SecurityContextTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DevolucionServiceImpl - tests unitarios")
class DevolucionServiceImplTest {

    @Mock private DevolucionRepository devolucionRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private AuditoriaPublisher auditoriaPublisher;
    @Mock private SecurityHelper securityHelper;
    @Mock private DevolucionesProperties properties;

    @InjectMocks private DevolucionServiceImpl service;

    private Pedido pedidoEntregado;
    private LineaPedido lineaPedido;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        SecurityContextTestUtils.autenticarComoAdmin();

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setEmail("c@test.local");
        usuario.setNombre("Cliente");

        cliente = new Cliente();
        cliente.setId(5L);
        cliente.setUsuario(usuario);

        Producto producto = new Producto();
        producto.setId(100L);
        producto.setNombre("Producto Test");
        producto.setStock(50);

        lineaPedido = new LineaPedido();
        lineaPedido.setId(200L);
        lineaPedido.setProducto(producto);
        lineaPedido.setCantidad(3);
        lineaPedido.setCantidadDevuelta(0);
        lineaPedido.setPrecioUnitario(new BigDecimal("10.00"));

        pedidoEntregado = new Pedido();
        pedidoEntregado.setId(1L);
        pedidoEntregado.setCliente(cliente);
        pedidoEntregado.setEstado(EstadoPedido.ENTREGADO);
        pedidoEntregado.setFechaPedido(LocalDateTime.now().minusDays(5));
        pedidoEntregado.setTotal(new BigDecimal("30.00"));
        List<LineaPedido> lineas = new ArrayList<>();
        lineas.add(lineaPedido);
        pedidoEntregado.setLineas(lineas);
        lineaPedido.setPedido(pedidoEntregado);
    }

    @AfterEach
    void tearDown() {
        SecurityContextTestUtils.limpiar();
    }

    // ============================================
    // crearDevolucion
    // ============================================

    @Test
    @DisplayName("crearDevolucion: exito con pedido ENTREGADO dentro de la ventana")
    void crearDevolucion_exito() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEntregado));
        when(properties.getDiasLimite()).thenReturn(14);
        when(devolucionRepository.save(any(Devolucion.class)))
            .thenAnswer(inv -> {
                Devolucion d = inv.getArgument(0);
                d.setId(99L);
                d.setFechaSolicitud(LocalDateTime.now());
                return d;
            });

        CrearDevolucionRequest req = new CrearDevolucionRequest();
        req.setPedidoId(1L);
        req.setMotivo("Producto defectuoso");
        LineaDevolucionRequest lr = new LineaDevolucionRequest();
        lr.setLineaPedidoId(200L);
        lr.setCantidad(2);
        req.setLineas(List.of(lr));

        DevolucionResponse resp = service.crearDevolucion(req);

        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getPedidoId()).isEqualTo(1L);
        assertThat(resp.getLineas()).hasSize(1);
        verify(auditoriaPublisher).publish(any(AuditoriaEvent.class));
    }

    @Test
    @DisplayName("crearDevolucion: falla si pedido no esta ENTREGADO")
    void crearDevolucion_falla_estadoIncorrecto() {
        pedidoEntregado.setEstado(EstadoPedido.PAGADO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEntregado));

        CrearDevolucionRequest req = new CrearDevolucionRequest();
        req.setPedidoId(1L);

        assertThatThrownBy(() -> service.crearDevolucion(req))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("ENTREGADO");

        verify(devolucionRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearDevolucion: falla fuera de la ventana de 14 dias")
    void crearDevolucion_falla_ventanaExpirada() {
        pedidoEntregado.setFechaPedido(LocalDateTime.now().minusDays(20));
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEntregado));
        when(properties.getDiasLimite()).thenReturn(14);

        CrearDevolucionRequest req = new CrearDevolucionRequest();
        req.setPedidoId(1L);

        assertThatThrownBy(() -> service.crearDevolucion(req))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("plazo");
    }

    @Test
    @DisplayName("crearDevolucion: falla si cantidad pedida supera lo devolvible")
    void crearDevolucion_falla_cantidadExcesiva() {
        lineaPedido.setCantidadDevuelta(2); // solo queda 1 devolvible
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEntregado));
        when(properties.getDiasLimite()).thenReturn(14);

        CrearDevolucionRequest req = new CrearDevolucionRequest();
        req.setPedidoId(1L);
        req.setMotivo("X");
        LineaDevolucionRequest lr = new LineaDevolucionRequest();
        lr.setLineaPedidoId(200L);
        lr.setCantidad(2);
        req.setLineas(List.of(lr));

        assertThatThrownBy(() -> service.crearDevolucion(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("devolvibles");
    }

    @Test
    @DisplayName("crearDevolucion: falla si pedido no existe")
    void crearDevolucion_falla_pedidoInexistente() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        CrearDevolucionRequest req = new CrearDevolucionRequest();
        req.setPedidoId(99L);

        assertThatThrownBy(() -> service.crearDevolucion(req))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crearDevolucion: cliente no dueno del pedido recibe AccessDenied")
    void crearDevolucion_falla_clienteNoDueno() {
        SecurityContextTestUtils.autenticarComoCliente();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedidoEntregado));
        when(securityHelper.getClienteActualId()).thenReturn(999L); // otro cliente

        CrearDevolucionRequest req = new CrearDevolucionRequest();
        req.setPedidoId(1L);

        assertThatThrownBy(() -> service.crearDevolucion(req))
            .isInstanceOf(AccessDeniedException.class);
    }

    // ============================================
    // aprobarDevolucion
    // ============================================

    @Test
    @DisplayName("aprobarDevolucion: restaura stock y publica evento")
    void aprobar_restauraStockYPublicaEvento() {
        Devolucion d = construirDevolucionSolicitada();
        when(devolucionRepository.findById(50L)).thenReturn(Optional.of(d));
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));

        int stockInicial = lineaPedido.getProducto().getStock();

        DevolucionResponse resp = service.aprobarDevolucion(50L);

        assertThat(resp.getEstado()).isEqualTo(EstadoDevolucion.APROBADA);
        assertThat(lineaPedido.getProducto().getStock()).isEqualTo(stockInicial + 2);
        assertThat(lineaPedido.getCantidadDevuelta()).isEqualTo(2);
        verify(auditoriaPublisher).publish(any(AuditoriaEvent.class));
    }

    @Test
    @DisplayName("aprobarDevolucion: falla si no esta en estado SOLICITADA")
    void aprobar_falla_estadoIncorrecto() {
        Devolucion d = construirDevolucionSolicitada();
        d.setEstado(EstadoDevolucion.APROBADA);
        when(devolucionRepository.findById(50L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.aprobarDevolucion(50L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SOLICITADA");
    }

    // ============================================
    // rechazarDevolucion
    // ============================================

    @Test
    @DisplayName("rechazarDevolucion: cambia estado y guarda motivo")
    void rechazar_exito() {
        Devolucion d = construirDevolucionSolicitada();
        when(devolucionRepository.findById(50L)).thenReturn(Optional.of(d));
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));

        RechazarDevolucionRequest req = new RechazarDevolucionRequest();
        req.setMotivoRechazo("Fuera de garantia");

        DevolucionResponse resp = service.rechazarDevolucion(50L, req);

        assertThat(resp.getEstado()).isEqualTo(EstadoDevolucion.RECHAZADA);
        assertThat(resp.getObservacionesAdmin()).isEqualTo("Fuera de garantia");
        verify(auditoriaPublisher).publish(any(AuditoriaEvent.class));
    }

    // ============================================
    // completarDevolucion
    // ============================================

    @Test
    @DisplayName("completarDevolucion: solo funciona desde APROBADA")
    void completar_falla_si_no_aprobada() {
        Devolucion d = construirDevolucionSolicitada();
        when(devolucionRepository.findById(50L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.completarDevolucion(50L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("APROBADAS");
    }

    @Test
    @DisplayName("completarDevolucion: APROBADA -> COMPLETADA emite evento")
    void completar_exito() {
        Devolucion d = construirDevolucionSolicitada();
        d.setEstado(EstadoDevolucion.APROBADA);
        when(devolucionRepository.findById(50L)).thenReturn(Optional.of(d));
        when(devolucionRepository.save(any(Devolucion.class))).thenAnswer(inv -> inv.getArgument(0));

        DevolucionResponse resp = service.completarDevolucion(50L);

        assertThat(resp.getEstado()).isEqualTo(EstadoDevolucion.COMPLETADA);
        verify(auditoriaPublisher).publish(any(AuditoriaEvent.class));
    }

    // ============================================
    // Helpers
    // ============================================

    private Devolucion construirDevolucionSolicitada() {
        Devolucion d = new Devolucion();
        d.setId(50L);
        d.setPedido(pedidoEntregado);
        d.setEstado(EstadoDevolucion.SOLICITADA);
        d.setMotivo("Test");
        d.setFechaSolicitud(LocalDateTime.now());

        LineaDevolucion ld = new LineaDevolucion(d, lineaPedido, 2, new BigDecimal("10.00"));
        ld.setId(300L);
        d.addLinea(ld);

        return d;
    }
}