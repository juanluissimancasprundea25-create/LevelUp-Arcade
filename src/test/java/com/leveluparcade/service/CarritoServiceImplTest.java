package com.leveluparcade.service;

import com.leveluparcade.entity.Carrito;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.LineaCarrito;
import com.leveluparcade.entity.Producto;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.CarritoRepository;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.repository.LineaCarritoRepository;
import com.leveluparcade.repository.ProductoRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.impl.CarritoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarritoServiceImpl - tests unitarios")
class CarritoServiceImplTest {

    @Mock private CarritoRepository carritoRepository;
    @Mock private LineaCarritoRepository lineaCarritoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks private CarritoServiceImpl service;

    private Cliente cliente;
    private Carrito carrito;
    private Producto producto;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setId(1L);

        carrito = Carrito.builder()
                .id(10L)
                .cliente(cliente)
                .lineas(new ArrayList<>())
                .build();

        producto = new Producto();
        producto.setId(100L);
        producto.setNombre("Mando Pro");
        producto.setPrecio(new BigDecimal("49.99"));
        producto.setStock(5);
        producto.setActivo(true);
    }

    // ---------- helper stub comun para obtenerCarritoActual ----------

    /**
     * Configura los mocks necesarios para que obtenerCarritoActual()
     * devuelva el carrito de setUp(). El service llama a
     * findByClienteIdConLineas (no findByClienteId) desde PR#23 hotfix LazyInit.
     */
    private void stubObtenerCarritoActual() {
        when(securityHelper.getClienteActualId()).thenReturn(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(carritoRepository.findByClienteIdConLineas(1L)).thenReturn(Optional.of(carrito));
    }

    // ---------- anadirProducto ----------

    @Test
    @DisplayName("anadir producto nuevo -> crea linea con la cantidad pedida")
    void anadirProductoNuevo() {
        stubObtenerCarritoActual();
        when(productoRepository.findById(100L)).thenReturn(Optional.of(producto));
        when(lineaCarritoRepository.findByCarritoIdAndProductoId(10L, 100L))
                .thenReturn(Optional.empty());
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));

        Carrito resultado = service.anadirProducto(100L, 2);

        assertThat(resultado.getLineas()).hasSize(1);
        assertThat(resultado.getLineas().get(0).getCantidad()).isEqualTo(2);
        assertThat(resultado.getTotalUnidades()).isEqualTo(2);
    }

    @Test
    @DisplayName("anadir producto ya existente -> suma a la cantidad existente")
    void anadirProductoExistenteSuma() {
        LineaCarrito existente = LineaCarrito.builder()
                .id(50L).carrito(carrito).producto(producto).cantidad(1).build();
        carrito.getLineas().add(existente);

        stubObtenerCarritoActual();
        when(productoRepository.findById(100L)).thenReturn(Optional.of(producto));
        when(lineaCarritoRepository.findByCarritoIdAndProductoId(10L, 100L))
                .thenReturn(Optional.of(existente));
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));

        Carrito resultado = service.anadirProducto(100L, 2);

        assertThat(resultado.getLineas()).hasSize(1);
        assertThat(resultado.getLineas().get(0).getCantidad()).isEqualTo(3); // 1 + 2
    }

    @Test
    @DisplayName("anadir mas que el stock -> IllegalArgumentException")
    void anadirSuperaStock() {
        stubObtenerCarritoActual();
        when(productoRepository.findById(100L)).thenReturn(Optional.of(producto));
        when(lineaCarritoRepository.findByCarritoIdAndProductoId(10L, 100L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.anadirProducto(100L, 6)) // stock=5
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stock");

        verify(carritoRepository, never()).save(any());
    }

    @Test
    @DisplayName("anadir cantidad cero o negativa -> IllegalArgumentException")
    void anadirCantidadInvalida() {
        assertThatThrownBy(() -> service.anadirProducto(100L, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("anadir producto inexistente -> ResourceNotFoundException")
    void anadirProductoInexistente() {
        when(productoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.anadirProducto(999L, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- cambiarCantidad ----------

    @Test
    @DisplayName("cambiar cantidad a 0 -> elimina la linea")
    void cambiarCantidadCeroElimina() {
        LineaCarrito linea = LineaCarrito.builder()
                .id(50L).carrito(carrito).producto(producto).cantidad(3).build();
        carrito.getLineas().add(linea);

        stubObtenerCarritoActual();
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));

        Carrito resultado = service.cambiarCantidad(50L, 0);

        assertThat(resultado.getLineas()).isEmpty();
    }

    @Test
    @DisplayName("cambiar cantidad por encima del stock -> IllegalArgumentException")
    void cambiarCantidadSuperaStock() {
        LineaCarrito linea = LineaCarrito.builder()
                .id(50L).carrito(carrito).producto(producto).cantidad(1).build();
        carrito.getLineas().add(linea);

        stubObtenerCarritoActual();

        assertThatThrownBy(() -> service.cambiarCantidad(50L, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stock");
    }

    @Test
    @DisplayName("cambiar cantidad de linea ajena -> ResourceNotFoundException")
    void cambiarLineaAjena() {
        stubObtenerCarritoActual();

        assertThatThrownBy(() -> service.cambiarCantidad(777L, 2))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- eliminarLinea ----------

    @Test
    @DisplayName("eliminar linea propia -> la quita del carrito")
    void eliminarLineaPropia() {
        LineaCarrito linea = LineaCarrito.builder()
                .id(50L).carrito(carrito).producto(producto).cantidad(2).build();
        carrito.getLineas().add(linea);

        stubObtenerCarritoActual();
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> inv.getArgument(0));

        Carrito resultado = service.eliminarLinea(50L);

        assertThat(resultado.getLineas()).isEmpty();
    }

    // ---------- contarUnidades ----------

    @Test
    @DisplayName("contar unidades sin cliente logueado -> 0")
    void contarSinCliente() {
        when(securityHelper.getClienteActualId()).thenReturn(null);

        assertThat(service.contarUnidades()).isZero();
        verify(carritoRepository, never()).findByClienteId(any());
    }

    @Test
    @DisplayName("contar unidades con carrito -> suma de cantidades")
    void contarConCarrito() {
        // contarUnidades usa findByClienteId (sin JOIN FETCH), no ConLineas.
        // Las lineas se acceden dentro de la transaccion -> no hay LazyInit.
        carrito.getLineas().add(LineaCarrito.builder()
                .id(50L).carrito(carrito).producto(producto).cantidad(2).build());
        carrito.getLineas().add(LineaCarrito.builder()
                .id(51L).carrito(carrito).producto(producto).cantidad(3).build());

        when(securityHelper.getClienteActualId()).thenReturn(1L);
        when(carritoRepository.findByClienteId(1L)).thenReturn(Optional.of(carrito));

        assertThat(service.contarUnidades()).isEqualTo(5);
    }

    // ---------- obtenerCarritoActual ----------

    @Test
    @DisplayName("obtener carrito cuando no existe -> lo crea")
    void obtenerCarritoCreaSiNoExiste() {
        when(securityHelper.getClienteActualId()).thenReturn(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        // findByClienteIdConLineas vacio -> crea nuevo
        when(carritoRepository.findByClienteIdConLineas(1L)).thenReturn(Optional.empty());
        when(carritoRepository.save(any(Carrito.class))).thenAnswer(inv -> {
            Carrito c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        Carrito resultado = service.obtenerCarritoActual();

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCliente()).isEqualTo(cliente);
        verify(carritoRepository).save(any(Carrito.class));
    }
}
