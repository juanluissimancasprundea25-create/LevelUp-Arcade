package com.leveluparcade.controller.web;

import com.leveluparcade.dto.request.EmitirFacturaRequest;
import com.leveluparcade.dto.response.FacturaResponse;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.service.FacturaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller web para la gestion de facturas desde el panel admin.
 *
 * <p>Tres responsabilidades:
 * <ol>
 *   <li>Listar y ver detalle de facturas existentes (ADMIN)</li>
 *   <li>Emitir factura desde un pedido (ADMIN, llamado desde el detalle del pedido)</li>
 *   <li>Verificacion publica de una factura por su numero (sin login)
 *       - util para el QR del PDF</li>
 * </ol>
 *
 * <p>La descarga del PDF tambien se sirve desde este controller para que la
 * URL sea consistente con el resto del panel web.
 */
@Controller
@RequestMapping("/admin/facturas")
public class FacturaWebController {

    private static final String SECCION = "facturas";

    private final FacturaService facturaService;

    public FacturaWebController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    /** Listado paginado de facturas. Solo ADMIN. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listar(
            @RequestParam(value = "clienteId", required = false) Long clienteId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "fechaEmision"));

        Page<FacturaResponse> pagina = facturaService.listarTodas(
                clienteId, null, null, pageable);

        model.addAttribute("pagina", pagina);
        model.addAttribute("facturas", pagina.getContent());
        model.addAttribute("clienteIdFiltro", clienteId);
        model.addAttribute("seccionActiva", SECCION);
        return "facturas/list";
    }

    /** Detalle de una factura. Solo ADMIN. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String detalle(@PathVariable Long id, Model model) {
        FacturaResponse factura = facturaService.obtenerPorId(id);
        model.addAttribute("factura", factura);
        model.addAttribute("seccionActiva", SECCION);
        return "facturas/detalle";
    }

    /** Emite una factura desde el id de un pedido. Solo ADMIN. */
    @PostMapping("/emitir/{pedidoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String emitirDesdePedido(
            @PathVariable Long pedidoId,
            RedirectAttributes ra) {
        try {
            EmitirFacturaRequest req = new EmitirFacturaRequest();
            req.setPedidoId(pedidoId);
            FacturaResponse f = facturaService.emitirFactura(req);
            ra.addFlashAttribute("flashOk",
                    "Factura " + f.getNumeroFactura() + " emitida correctamente.");
            return "redirect:/facturas/" + f.getId();
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/pedidos/" + pedidoId;
        }
    }

    /** Descarga el PDF de la factura. Solo ADMIN. */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        FacturaResponse factura = facturaService.obtenerPorId(id);
        byte[] pdf = facturaService.descargarPdf(id);
        String filename = "factura-" + factura.getNumeroFactura() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
