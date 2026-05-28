package com.leveluparcade.service.impl;

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
import com.leveluparcade.service.CarritoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Implementacion de {@link CarritoService}.
 *
 * <p>El carrito siempre pertenece al cliente autenticado, resuelto con
 * {@link SecurityHelper}. Si un usuario sin Cliente asociado (admin) llega
 * aqui, se lanza error: el carrito es de la tienda cliente.
 */
@Service
public class CarritoServiceImpl implements CarritoService {

    private static final Logger log = LoggerFactory.getLogger(CarritoServiceImpl.class);

    private final CarritoRepository carritoRepository;
    private final LineaCarritoRepository lineaCarritoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final SecurityHelper securityHelper;

    public CarritoServiceImpl(CarritoRepository carritoRepository,
                              LineaCarritoRepository lineaCarritoRepository,
                              ProductoRepository productoRepository,
                              ClienteRepository clienteRepository,
                              SecurityHelper securityHelper) {
        this.carritoRepository = carritoRepository;
        this.lineaCarritoRepository = lineaCarritoRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.securityHelper = securityHelper;
    }

    @Override
    @Transactional
    public Carrito obtenerCarritoActual() {
        Cliente cliente = clienteActual();
        // findByClienteIdConLineas hace LEFT JOIN FETCH de lineas+producto,
        // asi la vista puede acceder a ellas con open-in-view=false.
        return carritoRepository.findByClienteIdConLineas(cliente.getId())
                .orElseGet(() -> crearCarritoVacio(cliente));
    }

    @Override
    @Transactional
    public Carrito anadirProducto(Long productoId, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + productoId));

        if (Boolean.FALSE.equals(producto.getActivo())) {
            throw new IllegalArgumentException("El producto no esta disponible.");
        }

        Carrito carrito = obtenerCarritoActual();

        // Si ya existe linea de ese producto, sumamos; si no, creamos.
        LineaCarrito linea = lineaCarritoRepository
                .findByCarritoIdAndProductoId(carrito.getId(), productoId)
                .orElse(null);

        int cantidadActual = (linea != null && linea.getCantidad() != null) ? linea.getCantidad() : 0;
        int cantidadFinal = cantidadActual + cantidad;

        validarStock(producto, cantidadFinal);

        if (linea == null) {
            linea = LineaCarrito.builder()
                    .carrito(carrito)
                    .producto(producto)
                    .cantidad(cantidadFinal)
                    .build();
            carrito.getLineas().add(linea);
        } else {
            linea.setCantidad(cantidadFinal);
        }

        carritoRepository.save(carrito);
        log.info(">>> Carrito {}: producto {} cantidad -> {}", carrito.getId(), productoId, cantidadFinal);
        return carrito;
    }

    @Override
    @Transactional
    public Carrito cambiarCantidad(Long lineaId, int cantidad) {
        Carrito carrito = obtenerCarritoActual();
        LineaCarrito linea = buscarLineaPropia(carrito, lineaId);

        if (cantidad <= 0) {
            carrito.getLineas().remove(linea);
            carritoRepository.save(carrito);
            log.info(">>> Carrito {}: linea {} eliminada (cantidad 0)", carrito.getId(), lineaId);
            return carrito;
        }

        validarStock(linea.getProducto(), cantidad);
        linea.setCantidad(cantidad);
        carritoRepository.save(carrito);
        log.info(">>> Carrito {}: linea {} cantidad -> {}", carrito.getId(), lineaId, cantidad);
        return carrito;
    }

    @Override
    @Transactional
    public Carrito eliminarLinea(Long lineaId) {
        Carrito carrito = obtenerCarritoActual();
        LineaCarrito linea = buscarLineaPropia(carrito, lineaId);
        carrito.getLineas().remove(linea);
        carritoRepository.save(carrito);
        log.info(">>> Carrito {}: linea {} eliminada", carrito.getId(), lineaId);
        return carrito;
    }

    @Override
    @Transactional
    public void vaciar() {
        Carrito carrito = obtenerCarritoActual();
        carrito.getLineas().clear();
        carritoRepository.save(carrito);
        log.info(">>> Carrito {} vaciado", carrito.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public int contarUnidades() {
        Long clienteId = securityHelper.getClienteActualId();
        if (clienteId == null) {
            return 0;
        }
        return carritoRepository.findByClienteId(clienteId)
                .map(Carrito::getTotalUnidades)
                .orElse(0);
    }

    // ---------- helpers ----------

    /** Cliente autenticado o error si no hay (o es admin sin Cliente). */
    private Cliente clienteActual() {
        Long clienteId = securityHelper.getClienteActualId();
        if (clienteId == null) {
            throw new IllegalStateException("No hay un cliente autenticado.");
        }
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + clienteId));
    }

    private Carrito crearCarritoVacio(Cliente cliente) {
        Carrito carrito = Carrito.builder()
                .cliente(cliente)
                .lineas(new ArrayList<>())
                .build();
        return carritoRepository.save(carrito);
    }

    /** Comprueba que cantidad no supere el stock del producto. */
    private void validarStock(Producto producto, int cantidadDeseada) {
        int stock = producto.getStock() != null ? producto.getStock() : 0;
        if (cantidadDeseada > stock) {
            throw new IllegalArgumentException(
                    "No hay stock suficiente de \"" + producto.getNombre() + "\". "
                    + "Disponible: " + stock + ", solicitado: " + cantidadDeseada + ".");
        }
    }

    /** Busca una linea dentro del carrito; error si no pertenece a el. */
    private LineaCarrito buscarLineaPropia(Carrito carrito, Long lineaId) {
        return carrito.getLineas().stream()
                .filter(l -> l.getId() != null && l.getId().equals(lineaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La linea " + lineaId + " no pertenece a tu carrito."));
    }
}
