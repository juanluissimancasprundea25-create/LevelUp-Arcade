package com.leveluparcade.service.impl;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.config.DevolucionesProperties;
import com.leveluparcade.dto.request.CrearDevolucionRequest;
import com.leveluparcade.dto.request.LineaDevolucionRequest;
import com.leveluparcade.dto.request.RechazarDevolucionRequest;
import com.leveluparcade.dto.response.DevolucionResponse;
import com.leveluparcade.dto.response.LineaDevolucionResponse;
import com.leveluparcade.entity.*;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.DevolucionRepository;
import com.leveluparcade.repository.PedidoRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.DevolucionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DevolucionServiceImpl implements DevolucionService {

    private final DevolucionRepository devolucionRepository;
    private final PedidoRepository pedidoRepository;
    private final AuditoriaPublisher auditoriaPublisher;
    private final SecurityHelper securityHelper;
    private final DevolucionesProperties properties;

    public DevolucionServiceImpl(DevolucionRepository devolucionRepository,
                                 PedidoRepository pedidoRepository,
                                 AuditoriaPublisher auditoriaPublisher,
                                 SecurityHelper securityHelper,
                                 DevolucionesProperties properties) {
        this.devolucionRepository = devolucionRepository;
        this.pedidoRepository = pedidoRepository;
        this.auditoriaPublisher = auditoriaPublisher;
        this.securityHelper = securityHelper;
        this.properties = properties;
    }

    // ===========================================================
    // CREAR
    // ===========================================================
    @Override
    @Transactional
    public DevolucionResponse crearDevolucion(CrearDevolucionRequest request) {
        Pedido pedido = pedidoRepository.findById(request.getPedidoId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Pedido no encontrado: " + request.getPedidoId()));

        verificarPropiedadOAdmin(pedido);

        if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
            throw new IllegalStateException(
                "Solo se pueden devolver pedidos en estado ENTREGADO. " +
                "Estado actual: " + pedido.getEstado());
        }

        // Ventana temporal: usamos fechaPedido como aproximacion ya que
        // en V1 no guardamos fecha_entrega. Cuando se anada esa columna
        // (futura migracion) cambiar aqui a pedido.getFechaEntrega().
        LocalDateTime fechaLimite = pedido.getFechaPedido()
            .plusDays(properties.getDiasLimite());
        if (LocalDateTime.now().isAfter(fechaLimite)) {
            throw new IllegalStateException(
                "Ha expirado el plazo de devolucion (" +
                properties.getDiasLimite() + " dias).");
        }

        // Indexar lineas por id para validar rapido lo que pide el cliente
        Map<Long, LineaPedido> lineasPedidoPorId = new HashMap<>();
        for (LineaPedido lp : pedido.getLineas()) {
            lineasPedidoPorId.put(lp.getId(), lp);
        }

        Devolucion devolucion = new Devolucion();
        devolucion.setPedido(pedido);
        devolucion.setMotivo(request.getMotivo());

        for (LineaDevolucionRequest req : request.getLineas()) {
            LineaPedido lp = lineasPedidoPorId.get(req.getLineaPedidoId());
            if (lp == null) {
                throw new IllegalArgumentException(
                    "La linea " + req.getLineaPedidoId() +
                    " no pertenece al pedido " + pedido.getId());
            }
            int devolvible = lp.getCantidadDevolvible();
            if (req.getCantidad() > devolvible) {
                throw new IllegalArgumentException(
                    "No se pueden devolver " + req.getCantidad() +
                    " unidades de la linea " + lp.getId() +
                    ": solo quedan " + devolvible + " devolvibles.");
            }
            LineaDevolucion ld = new LineaDevolucion(
                devolucion, lp, req.getCantidad(), lp.getPrecioUnitario());
            devolucion.addLinea(ld);
        }

        Devolucion guardada = devolucionRepository.save(devolucion);

        auditoriaPublisher.publish(new AuditoriaEvent(
            TipoEvento.DEVOLUCION_SOLICITADA,
            "Devolucion",
            guardada.getId(),
            securityHelper.getUsuarioActualId(),
            "Devolucion solicitada para pedido " + pedido.getId()
        ));

        return toResponse(guardada);
    }

    // ===========================================================
    // LISTADOS
    // ===========================================================
    @Override
    @Transactional(readOnly = true)
    public Page<DevolucionResponse> listarDevoluciones(EstadoDevolucion estado,
                                                      Long clienteId,
                                                      Pageable pageable) {
        return devolucionRepository.buscarConFiltros(estado, clienteId, pageable)
            .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DevolucionResponse> listarMisDevoluciones(Pageable pageable) {
        Long clienteId = securityHelper.getClienteActualId();
        if (clienteId == null) {
            throw new AccessDeniedException("Solo accesible por clientes");
        }
        return devolucionRepository.findByClienteId(clienteId, pageable)
            .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DevolucionResponse obtenerDevolucion(Long id) {
        Devolucion d = cargarDevolucion(id);
        verificarPropiedadOAdmin(d.getPedido());
        return toResponse(d);
    }

    // ===========================================================
    // TRANSICIONES DE ESTADO (ADMIN; restringido por el controller)
    // ===========================================================
    @Override
    @Transactional
    public DevolucionResponse aprobarDevolucion(Long id) {
        Devolucion d = cargarDevolucion(id);

        if (d.getEstado() != EstadoDevolucion.SOLICITADA) {
            throw new IllegalStateException(
                "Solo se pueden aprobar devoluciones en estado SOLICITADA. " +
                "Estado actual: " + d.getEstado());
        }

        for (LineaDevolucion ld : d.getLineas()) {
            LineaPedido lp = ld.getLineaPedido();

            int nuevaDevuelta = lp.getCantidadDevuelta() + ld.getCantidad();
            if (nuevaDevuelta > lp.getCantidad()) {
                throw new IllegalStateException(
                    "Inconsistencia: cantidad devuelta excede la pedida " +
                    "en linea " + lp.getId());
            }
            lp.setCantidadDevuelta(nuevaDevuelta);

            // Restaurar stock
            Producto producto = lp.getProducto();
            producto.setStock(producto.getStock() + ld.getCantidad());
        }

        d.setEstado(EstadoDevolucion.APROBADA);
        d.setImporteDevuelto(d.calcularImporte());

        Devolucion actualizada = devolucionRepository.save(d);

        auditoriaPublisher.publish(new AuditoriaEvent(
            TipoEvento.DEVOLUCION_APROBADA,
            "Devolucion",
            actualizada.getId(),
            securityHelper.getUsuarioActualId(),
            "Devolucion aprobada. Importe: " + actualizada.getImporteDevuelto()
        ));

        return toResponse(actualizada);
    }

    @Override
    @Transactional
    public DevolucionResponse rechazarDevolucion(Long id, RechazarDevolucionRequest request) {
        Devolucion d = cargarDevolucion(id);

        if (d.getEstado() != EstadoDevolucion.SOLICITADA) {
            throw new IllegalStateException(
                "Solo se pueden rechazar devoluciones en estado SOLICITADA. " +
                "Estado actual: " + d.getEstado());
        }

        d.setEstado(EstadoDevolucion.RECHAZADA);
        d.setObservacionesAdmin(request.getMotivoRechazo());

        Devolucion actualizada = devolucionRepository.save(d);

        auditoriaPublisher.publish(new AuditoriaEvent(
            TipoEvento.DEVOLUCION_RECHAZADA,
            "Devolucion",
            actualizada.getId(),
            securityHelper.getUsuarioActualId(),
            "Devolucion rechazada: " + request.getMotivoRechazo()
        ));

        return toResponse(actualizada);
    }

    @Override
    @Transactional
    public DevolucionResponse completarDevolucion(Long id) {
        Devolucion d = cargarDevolucion(id);

        if (d.getEstado() != EstadoDevolucion.APROBADA) {
            throw new IllegalStateException(
                "Solo se pueden completar devoluciones APROBADAS. " +
                "Estado actual: " + d.getEstado());
        }

        d.setEstado(EstadoDevolucion.COMPLETADA);
        Devolucion actualizada = devolucionRepository.save(d);

        auditoriaPublisher.publish(new AuditoriaEvent(
            TipoEvento.DEVOLUCION_COMPLETADA,
            "Devolucion",
            actualizada.getId(),
            securityHelper.getUsuarioActualId(),
            "Devolucion completada. Reembolso realizado."
        ));

        return toResponse(actualizada);
    }

    // ===========================================================
    // HELPERS
    // ===========================================================

    private Devolucion cargarDevolucion(Long id) {
        return devolucionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Devolucion no encontrada: " + id));
    }

    private void verificarPropiedadOAdmin(Pedido pedido) {
        if (esAdmin()) {
            return;
        }
        Long clienteActual = securityHelper.getClienteActualId();
        if (clienteActual == null || !clienteActual.equals(pedido.getCliente().getId())) {
            throw new AccessDeniedException(
                "No tienes permiso sobre este pedido");
        }
    }

    private boolean esAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    // ===========================================================
    // MAPPING
    // ===========================================================
    private DevolucionResponse toResponse(Devolucion d) {
        DevolucionResponse r = new DevolucionResponse();
        r.setId(d.getId());
        r.setPedidoId(d.getPedido().getId());
        r.setClienteId(d.getPedido().getCliente().getId());
        r.setFechaSolicitud(d.getFechaSolicitud());
        r.setMotivo(d.getMotivo());
        r.setEstado(d.getEstado());
        r.setImporteDevuelto(d.getImporteDevuelto());
        r.setObservacionesAdmin(d.getObservacionesAdmin());

        List<LineaDevolucionResponse> lineas = new ArrayList<>();
        for (LineaDevolucion ld : d.getLineas()) {
            lineas.add(toLineaResponse(ld));
        }
        r.setLineas(lineas);
        return r;
    }

    private LineaDevolucionResponse toLineaResponse(LineaDevolucion ld) {
        LineaDevolucionResponse r = new LineaDevolucionResponse();
        r.setId(ld.getId());
        r.setLineaPedidoId(ld.getLineaPedido().getId());
        r.setProductoId(ld.getLineaPedido().getProducto().getId());
        r.setProductoNombre(ld.getLineaPedido().getProducto().getNombre());
        r.setCantidad(ld.getCantidad());
        r.setPrecioUnitario(ld.getPrecioUnitario());
        r.setSubtotal(ld.getSubtotal());
        return r;
    }
}