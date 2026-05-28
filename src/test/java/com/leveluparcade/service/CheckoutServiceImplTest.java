package com.leveluparcade.service;

import com.leveluparcade.dto.request.EmitirFacturaRequest;
import com.leveluparcade.dto.request.PagarPedidoRequest;
import com.leveluparcade.dto.request.PedidoCreateRequest;
import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.Carrito;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.EstadoPedido;
import com.leveluparcade.entity.LineaCarrito;
import com.leveluparcade.entity.MetodoPago;
import com.leveluparcade.entity.Producto;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.impl.CheckoutServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutServiceImpl - tests unitarios")
class CheckoutServiceImplTest {

    @Mock private CarritoService carritoService;
    @Mock private PedidoService pedidoService;
    @Mock private FacturaService facturaService;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks private CheckoutServiceImpl service;

    private Carrito carritoConLineas;

    @BeforeEach
    void setUp() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        Producto producto = new Producto();
        producto.setId(100L);
        producto.setNombre("Mando Pro");
        producto.setPrecio(new BigDecimal("49.99"));
        producto.setStock(10);
        producto.setActivo(true);

        carritoConLineas = Carrito.builder()
                .id(10L)
                .cliente(cliente)
                .lineas(new ArrayList<>())
                .build();
        carritoConLineas.getLineas().add(LineaCarrito.builder()
                .id(50L).carrito(carritoConLineas).producto(producto).cantidad(2).build());
    }

    private PedidoResponse pedidoFicticio(EstadoPedido estado) {
        return new PedidoResponse(
                500L, 1L, "cli@test.com", "Cliente Test",
                LocalDateTime.now(), estado, new BigDecimal("99.98"),
                MetodoPago.TARJETA, "Calle Falsa 123", List.of());
    }

    @Test
    @DisplayName("checkout OK -> crea, paga, emite factura y vacia carrito en orden")
    void checkoutOk() {
        when(securityHelper.getClienteActualId()).thenReturn(1L);
        when(carritoService.obtenerCarritoActual()).thenReturn(carritoConLineas);
        when(pedidoService.crear(eq(1L), any(PedidoCreateRequest.class)))
                .thenReturn(pedidoFicticio(EstadoPedido.PENDIENTE));
        when(pedidoService.pagar(eq(500L), any(PagarPedidoRequest.class)))
                .thenReturn(pedidoFicticio(EstadoPedido.PAGADO));

        PedidoResponse resultado = service.procesarCheckout(MetodoPago.TARJETA, "Calle Falsa 123");

        assertThat(resultado.estado()).isEqualTo(EstadoPedido.PAGADO);

        // Orden correcto: crear -> pagar -> emitir factura -> vaciar
        InOrder orden = inOrder(pedidoService, facturaService, carritoService);
        orden.verify(pedidoService).crear(eq(1L), any(PedidoCreateRequest.class));
        orden.verify(pedidoService).pagar(eq(500L), any(PagarPedidoRequest.class));
        orden.verify(facturaService).emitirFactura(any(EmitirFacturaRequest.class));
        orden.verify(carritoService).vaciar();
    }

    @Test
    @DisplayName("checkout con carrito vacio -> IllegalState y NO vacia ni crea pedido")
    void checkoutCarritoVacio() {
        Carrito vacio = Carrito.builder()
                .id(11L).lineas(new ArrayList<>()).build();
        when(securityHelper.getClienteActualId()).thenReturn(1L);
        when(carritoService.obtenerCarritoActual()).thenReturn(vacio);

        assertThatThrownBy(() -> service.procesarCheckout(MetodoPago.PAYPAL, "dir"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vacio");

        verify(pedidoService, never()).crear(any(), any());
        verify(carritoService, never()).vaciar();
    }

    @Test
    @DisplayName("checkout sin cliente logueado -> IllegalState")
    void checkoutSinCliente() {
        when(securityHelper.getClienteActualId()).thenReturn(null);

        assertThatThrownBy(() -> service.procesarCheckout(MetodoPago.TARJETA, "dir"))
                .isInstanceOf(IllegalStateException.class);

        verify(carritoService, never()).obtenerCarritoActual();
    }

    @Test
    @DisplayName("si pagar falla por stock -> propaga y NO emite factura ni vacia")
    void checkoutFallaAlPagar() {
        when(securityHelper.getClienteActualId()).thenReturn(1L);
        when(carritoService.obtenerCarritoActual()).thenReturn(carritoConLineas);
        when(pedidoService.crear(eq(1L), any(PedidoCreateRequest.class)))
                .thenReturn(pedidoFicticio(EstadoPedido.PENDIENTE));
        when(pedidoService.pagar(eq(500L), any(PagarPedidoRequest.class)))
                .thenThrow(new IllegalStateException("Stock insuficiente al pagar"));

        assertThatThrownBy(() -> service.procesarCheckout(MetodoPago.TARJETA, "dir"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock");

        verify(facturaService, never()).emitirFactura(any());
        verify(carritoService, never()).vaciar();
    }
}
