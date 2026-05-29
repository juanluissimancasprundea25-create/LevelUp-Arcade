package com.leveluparcade.controller.api;

import com.leveluparcade.dto.request.EmitirFacturaRequest;
import com.leveluparcade.dto.response.FacturaResponse;
import com.leveluparcade.service.FacturaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/facturas")
public class FacturaApiController {

    private final FacturaService facturaService;

    public FacturaApiController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacturaResponse> emitir(@Valid @RequestBody EmitirFacturaRequest request) {
        return ResponseEntity.ok(facturaService.emitirFactura(request));
    }

    @PostMapping("/desde-pedido/{pedidoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FacturaResponse> emitirDesdePedido(@PathVariable Long pedidoId) {
        EmitirFacturaRequest req = new EmitirFacturaRequest();
        req.setPedidoId(pedidoId);
        return ResponseEntity.ok(facturaService.emitirFactura(req));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<FacturaResponse>> listarTodas(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            Pageable pageable) {
        return ResponseEntity.ok(facturaService.listarTodas(clienteId, desde, hasta, pageable));
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Page<FacturaResponse>> listarMias(Pageable pageable) {
        return ResponseEntity.ok(facturaService.listarMias(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<FacturaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.obtenerPorId(id));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','CLIENTE')")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        byte[] pdf = facturaService.descargarPdf(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factura-" + id + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}