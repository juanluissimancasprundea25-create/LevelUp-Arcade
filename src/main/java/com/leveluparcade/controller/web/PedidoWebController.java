package com.leveluparcade.controller.web;

import com.leveluparcade.dto.request.PagarPedidoRequest;
import com.leveluparcade.dto.response.PedidoResponse;
import com.leveluparcade.entity.EstadoPedido;
import com.leveluparcade.entity.MetodoPago;
import com.leveluparcade.service.PedidoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller web para la gestion de pedidos desde el panel admin.
 *
 * <p>Vista read-only + acciones de transicion de estado
 * (pagar, enviar, entregar, cancelar). El alta de pedidos se hace
 * desde el frontend de cliente (otro PR), no desde el panel admin.
 *
 * <p>Llama directamente al PedidoService (Opcion A), reutilizando la
 * misma logica de negocio que la API REST {@code /api/pedidos}.
 */
@Controller
@RequestMapping("/admin/pedidos")
@PreAuthorize("hasRole('ADMIN')")
public class PedidoWebController {

    private static final String SECCION = "pedidos";

    private final PedidoService pedidoService;

    public PedidoWebController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /** Listado con filtro opcional por estado. */
    @GetMapping
    public String listar(
            @RequestParam(value = "estado", required = false) EstadoPedido estado,
            Model model) {

        List<PedidoResponse> pedidos = (estado != null)
                ? pedidoService.listarPorEstado(estado)
                : pedidoService.listarTodos();

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("estados", EstadoPedido.values());
        model.addAttribute("seccionActiva", SECCION);
        return "pedidos/list";
    }

    /** Detalle de un pedido. */
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        PedidoResponse pedido = pedidoService.obtenerPorId(id);
        model.addAttribute("pedido", pedido);
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("seccionActiva", SECCION);
        return "pedidos/detalle";
    }

    /** Marca el pedido como PAGADO. */
    @PostMapping("/{id}/pagar")
    public String pagar(
            @PathVariable Long id,
            @RequestParam MetodoPago metodoPago,
            RedirectAttributes ra) {
        try {
            pedidoService.pagar(id, new PagarPedidoRequest(metodoPago));
            ra.addFlashAttribute("flashOk",
                    "Pedido marcado como PAGADO (" + metodoPago + ").");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    /** Marca el pedido como ENVIADO. */
    @PostMapping("/{id}/enviar")
    public String enviar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            pedidoService.enviar(id);
            ra.addFlashAttribute("flashOk", "Pedido marcado como ENVIADO.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    /** Marca el pedido como ENTREGADO. */
    @PostMapping("/{id}/entregar")
    public String entregar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            pedidoService.entregar(id);
            ra.addFlashAttribute("flashOk", "Pedido marcado como ENTREGADO.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }

    /** Cancela el pedido (restaura stock si estaba PAGADO). */
    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            pedidoService.cancelar(id);
            ra.addFlashAttribute("flashOk", "Pedido CANCELADO.");
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
        }
        return "redirect:/pedidos/" + id;
    }
}