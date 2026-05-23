package com.leveluparcade.service.impl;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.dto.request.LineaPedidoRequest;
import com.leveluparcade.dto.request.PagarPedidoRequest;
import com.leveluparcade.dto.request.PedidoCreateRequest;
import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.Cliente;
import com.leveluparcade.entity.EstadoPedido;
import com.leveluparcade.entity.LineaPedido;
import com.leveluparcade.entity.Pedido;
import com.leveluparcade.entity.Producto;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.ClienteRepository;
import com.leveluparcade.repository.PedidoRepository;
import com.leveluparcade.repository.ProductoRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion de {@link PedidoService}.
 *
 * <p>Las transiciones de estado se validan explicitamente; no existe un
 * setEstado generico. Esto previene saltos invalidos (ej: PENDIENTE -> ENVIADO).
 */
@Service
public class PedidoServiceImpl implements PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final AuditoriaPublisher auditoria;
    private final SecurityHelper securityHelper;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             ClienteRepository clienteRepository,
                             ProductoRepository productoRepository,
                             AuditoriaPublisher auditoria,
                             SecurityHelper securityHelper) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.auditoria = auditoria;
        this.securityHelper = securityHelper;
    }

    // ---------- Consultas ----------

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarTodos() {
        return pedidoRepository.findAll().stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorEstado(EstadoPedido estado) {
        return pedidoRepository.findByEstadoOrderByFechaPedidoDesc(estado).stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarDeCliente(Long clienteId) {
        return pedidoRepository.findByClienteIdOrderByFechaPedidoDesc(clienteId).stream()
                .map(PedidoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtenerPorId(Long id) {
        return PedidoResponse.from(buscarPedidoOExcepcion(id));
    }

    // ---------- Crear ----------

    @Override
    @Transactional
    public PedidoResponse crear(Long clienteId, PedidoCreateRequest request) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .estado(EstadoPedido.PENDIENTE)
                .direccionEnvio(request.direccionEnvio())
                .build();

        // Para cada linea: cargar producto, validar stock, copiar precio
        for (LineaPedidoRequest lineaReq : request.lineas()) {
            Producto producto = productoRepository.findById(lineaReq.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto", lineaReq.productoId()));

            if (producto.getStock() < lineaReq.cantidad()) {
                throw new IllegalArgumentException(
                    "Stock insuficiente para '" + producto.getNombre() +
                    "' (SKU " + producto.getSku() + "): solicitado " +
                    lineaReq.cantidad() + ", disponible " + producto.getStock());
            }

            LineaPedido linea = LineaPedido.builder()
                    .producto(producto)
                    .cantidad(lineaReq.cantidad())
                    .precioUnitario(producto.getPrecio())   // snapshot
                    .build();

            pedido.anadirLinea(linea);   // mantiene bidireccionalidad y recalcula total
        }

        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido creado: id={}, cliente={}, total={}",
                guardado.getId(), clienteId, guardado.getTotal());

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.PEDIDO_CREADO, "Pedido",
            guardado.getId(), securityHelper.getUsuarioActualId(),
            "Pedido creado por cliente id=" + clienteId +
            " con " + guardado.getLineas().size() + " linea(s), total=" + guardado.getTotal()));

        return PedidoResponse.from(guardado);
    }

    // ---------- Transiciones de estado ----------

    @Override
    @Transactional
    public PedidoResponse pagar(Long id, PagarPedidoRequest request) {
        Pedido pedido = buscarPedidoOExcepcion(id);

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException(
                "Solo se puede pagar un pedido en estado PENDIENTE. " +
                "Estado actual: " + pedido.getEstado());
        }

        // Re-validar stock en el momento del pago (puede haber cambiado
        // desde la creacion). Descontar atomicamente.
        for (LineaPedido linea : pedido.getLineas()) {
            Producto producto = linea.getProducto();
            if (producto.getStock() < linea.getCantidad()) {
                throw new IllegalStateException(
                    "Stock insuficiente al pagar para '" + producto.getNombre() +
                    "': solicitado " + linea.getCantidad() +
                    ", disponible " + producto.getStock());
            }
            producto.setStock(producto.getStock() - linea.getCantidad());
            productoRepository.save(producto);
        }

        pedido.setEstado(EstadoPedido.PAGADO);
        pedido.setMetodoPago(request.metodoPago());

        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("Pedido pagado: id={}, metodo={}", id, request.metodoPago());

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.PEDIDO_PAGADO, "Pedido", id,
            securityHelper.getUsuarioActualId(),
            "Pago confirmado con " + request.metodoPago() +
            ", total=" + pedido.getTotal()));

        return PedidoResponse.from(actualizado);
    }

    @Override
    @Transactional
    public PedidoResponse enviar(Long id) {
        Pedido pedido = buscarPedidoOExcepcion(id);

        if (pedido.getEstado() != EstadoPedido.PAGADO) {
            throw new IllegalStateException(
                "Solo se puede enviar un pedido en estado PAGADO. " +
                "Estado actual: " + pedido.getEstado());
        }

        pedido.setEstado(EstadoPedido.ENVIADO);
        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("Pedido enviado: id={}", id);

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.PEDIDO_ENVIADO, "Pedido", id,
            securityHelper.getUsuarioActualId(),
            "Pedido marcado como enviado"));

        return PedidoResponse.from(actualizado);
    }

    @Override
    @Transactional
    public PedidoResponse entregar(Long id) {
        Pedido pedido = buscarPedidoOExcepcion(id);

        if (pedido.getEstado() != EstadoPedido.ENVIADO) {
            throw new IllegalStateException(
                "Solo se puede entregar un pedido en estado ENVIADO. " +
                "Estado actual: " + pedido.getEstado());
        }

        pedido.setEstado(EstadoPedido.ENTREGADO);
        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("Pedido entregado: id={}", id);

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.PEDIDO_ENTREGADO, "Pedido", id,
            securityHelper.getUsuarioActualId(),
            "Pedido marcado como entregado"));

        return PedidoResponse.from(actualizado);
    }

    @Override
    @Transactional
    public PedidoResponse cancelar(Long id) {
        Pedido pedido = buscarPedidoOExcepcion(id);

        EstadoPedido estadoPrevio = pedido.getEstado();

        if (estadoPrevio != EstadoPedido.PENDIENTE && estadoPrevio != EstadoPedido.PAGADO) {
            throw new IllegalStateException(
                "Solo se puede cancelar un pedido PENDIENTE o PAGADO. " +
                "Estado actual: " + estadoPrevio);
        }

        // Si estaba pagado, devolver el stock que se descontO al pagar
        if (estadoPrevio == EstadoPedido.PAGADO) {
            for (LineaPedido linea : pedido.getLineas()) {
                Producto producto = linea.getProducto();
                producto.setStock(producto.getStock() + linea.getCantidad());
                productoRepository.save(producto);
            }
            log.info("Stock restaurado tras cancelar pedido id={}", id);
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        Pedido actualizado = pedidoRepository.save(pedido);
        log.info("Pedido cancelado: id={}, estado previo={}", id, estadoPrevio);

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.PEDIDO_CANCELADO, "Pedido", id,
            securityHelper.getUsuarioActualId(),
            "Pedido cancelado desde estado " + estadoPrevio));

        return PedidoResponse.from(actualizado);
    }

    // ---------- helpers ----------

    private Pedido buscarPedidoOExcepcion(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido", id));
    }
}