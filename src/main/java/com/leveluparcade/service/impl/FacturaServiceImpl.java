package com.leveluparcade.service.impl;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.dto.request.EmitirFacturaRequest;
import com.leveluparcade.dto.response.FacturaResponse;
import com.leveluparcade.entity.Factura;
import com.leveluparcade.entity.Pedido;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.FacturaRepository;
import com.leveluparcade.repository.PedidoRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.FacturaService;
import com.leveluparcade.service.NumeradorFacturas;
import com.leveluparcade.service.PdfFacturaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FacturaServiceImpl implements FacturaService {

    private final FacturaRepository facturaRepository;
    private final PedidoRepository pedidoRepository;
    private final NumeradorFacturas numerador;
    private final PdfFacturaService pdfService;
    private final SecurityHelper securityHelper;
    private final AuditoriaPublisher auditoria;

    public FacturaServiceImpl(
            FacturaRepository facturaRepository,
            PedidoRepository pedidoRepository,
            NumeradorFacturas numerador,
            PdfFacturaService pdfService,
            SecurityHelper securityHelper,
            AuditoriaPublisher auditoria) {
        this.facturaRepository = facturaRepository;
        this.pedidoRepository = pedidoRepository;
        this.numerador = numerador;
        this.pdfService = pdfService;
        this.securityHelper = securityHelper;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public FacturaResponse emitirFactura(EmitirFacturaRequest request) {
        Pedido pedido = pedidoRepository.findById(request.getPedidoId())
            .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + request.getPedidoId()));

        // Idempotencia: si ya existe factura, devolverla tal cual
        return facturaRepository.findByPedidoId(pedido.getId())
            .map(this::toResponse)
            .orElseGet(() -> crearFactura(pedido));
    }

    private FacturaResponse crearFactura(Pedido pedido) {
        // Validacion: solo pedidos PAGADO, ENVIADO o ENTREGADO son facturables
        String estado = pedido.getEstado().name();
        if (!(estado.equals("PAGADO") || estado.equals("ENVIADO") || estado.equals("ENTREGADO"))) {
            throw new IllegalStateException(
                "Solo se pueden facturar pedidos pagados, enviados o entregados. Estado actual: " + estado);
        }

        String numero = numerador.generarSiguiente();

        Factura factura = new Factura();
        factura.setPedido(pedido);
        factura.setNumeroFactura(numero);
        factura.setFechaEmision(LocalDateTime.now());

        Factura guardada = facturaRepository.save(factura);

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.FACTURA_EMITIDA,
            "Factura",
            guardada.getId(),
            securityHelper.getUsuarioActualId(),
            "Factura " + numero + " emitida para pedido " + pedido.getId()
        ));

        return toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaResponse obtenerPorId(Long id) {
        Factura f = buscarYVerificarAcceso(id);
        return toResponse(f);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FacturaResponse> listarTodas(Long clienteId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        return facturaRepository.buscarConFiltros(clienteId, desde, hasta, pageable)
            .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FacturaResponse> listarMias(Pageable pageable) {
        Long clienteId = securityHelper.getClienteActualId();
        return facturaRepository.findByPedidoClienteId(clienteId, pageable)
            .map(this::toResponse);
    }

    @Override
    @Transactional
    public byte[] descargarPdf(Long id) {
        Factura f = buscarYVerificarAcceso(id);
        byte[] pdf = pdfService.generar(f);

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.FACTURA_DESCARGADA,
            "Factura",
            f.getId(),
            securityHelper.getUsuarioActualId(),
            "Factura " + f.getNumeroFactura() + " descargada"
        ));

        return pdf;
    }

    /**
     * Busca factura y verifica que el usuario actual sea ADMIN o el cliente dueño.
     */
    private Factura buscarYVerificarAcceso(Long id) {
        Factura f = facturaRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada: " + id));

        if (securityHelper.esAdmin()) {
            return f;
        }
        Long clienteActualId = securityHelper.getClienteActualId();
        if (clienteActualId == null || !clienteActualId.equals(f.getPedido().getCliente().getId())) {
            throw new AccessDeniedException("No tienes permiso para acceder a esta factura");
        }
        return f;
    }

    private FacturaResponse toResponse(Factura f) {
        FacturaResponse r = new FacturaResponse();
        r.setId(f.getId());
        r.setNumeroFactura(f.getNumeroFactura());
        r.setFechaEmision(f.getFechaEmision());
        r.setPedidoId(f.getPedido().getId());
        r.setClienteId(f.getPedido().getCliente().getId());
        r.setClienteNombre(
            f.getPedido().getCliente().getUsuario().getNombre() +
            (f.getPedido().getCliente().getUsuario().getApellidos() != null
                ? " " + f.getPedido().getCliente().getUsuario().getApellidos() : "")
        );
        r.setTotal(f.getPedido().getTotal());
        return r;
    }
}