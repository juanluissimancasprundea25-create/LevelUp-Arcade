package com.leveluparcade.controller.web;

import com.leveluparcade.dto.request.RechazarDevolucionRequest;
import com.leveluparcade.dto.response.DevolucionResponse;
import com.leveluparcade.entity.EstadoDevolucion;
import com.leveluparcade.service.DevolucionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
 * Controller web para gestion de devoluciones desde el panel admin.
 *
 * <p>Read-only + acciones de transicion de estado (aprobar, rechazar,
 * completar). El alta la hacen los clientes desde su frontend.
 *
 * <p>Llama directamente al DevolucionService (Opcion A).
 */
@Controller
@RequestMapping("/admin/devoluciones")
@PreAuthorize("hasRole('ADMIN')")
public class DevolucionWebController {

    private static final String SECCION = "devoluciones";

    private final DevolucionService devolucionService;

    public DevolucionWebController(DevolucionService devolucionService) {
        this.devolucionService = devolucionService;
    }

    /** Listado paginado con filtros opcionales por estado y cliente. */
    @GetMapping
    public String listar(
            @RequestParam(value = "estado", required = false) EstadoDevolucion estado,
            @RequestParam(value = "clienteId", required = false) Long clienteId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "fechaSolicitud"));

        Page<DevolucionResponse> pagina =
                devolucionService.listarDevoluciones(estado, clienteId, pageable);

        model.addAttribute("pagina", pagina);
        model.addAttribute("devoluciones", pagina.getContent());
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("clienteIdFiltro", clienteId);
        model.addAttribute("estados", EstadoDevolucion.values());
        model.addAttribute("seccionActiva", SECCION);
        return "devoluciones/list";
    }

    /** Detalle de una devolucion. */
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        DevolucionResponse devolucion = devolucionService.obtenerDevolucion(id);
        model.addAttribute("devolucion", devolucion);
        model.addAttribute("seccionActiva", SECCION);
        return "devoluciones/detalle";
    }

    /** Aprueba la devolucion (cambia estado, calcula importe, restaura stock). */
    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            DevolucionResponse d = devolucionService.aprobarDevolucion(id);
            ra.addFlashAttribute("flashOk",
                    "Devolucion APROBADA. Importe a reembolsar: "
                            + d.getImporteDevuelto() + " EUR.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/devoluciones/" + id;
    }

    /** Rechaza la devolucion con el motivo recibido del form. */
    @PostMapping("/{id}/rechazar")
    public String rechazar(
            @PathVariable Long id,
            @RequestParam("motivoRechazo") String motivoRechazo,
            RedirectAttributes ra) {

        if (motivoRechazo == null || motivoRechazo.isBlank()) {
            ra.addFlashAttribute("flashError",
                    "Debes indicar un motivo para rechazar la devolucion.");
            return "redirect:/devoluciones/" + id;
        }

        try {
            RechazarDevolucionRequest req = new RechazarDevolucionRequest();
            req.setMotivoRechazo(motivoRechazo);
            devolucionService.rechazarDevolucion(id, req);
            ra.addFlashAttribute("flashOk", "Devolucion RECHAZADA.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/devoluciones/" + id;
    }

    /** Marca la devolucion como COMPLETADA (tras reembolso manual). */
    @PostMapping("/{id}/completar")
    public String completar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            devolucionService.completarDevolucion(id);
            ra.addFlashAttribute("flashOk", "Devolucion COMPLETADA.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/devoluciones/" + id;
    }
}