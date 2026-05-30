package com.leveluparcade.controller.web;

import com.leveluparcade.dto.request.ClienteCreateRequest;
import com.leveluparcade.dto.request.ClienteUpdateRequest;
import com.leveluparcade.dto.response.ClienteCreadoResponse;
import com.leveluparcade.dto.response.ClienteResponse;
import com.leveluparcade.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/clientes")
@PreAuthorize("hasRole('ADMIN')")
public class ClienteWebController {

    private final ClienteService clienteService;

    public ClienteWebController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(@RequestParam(value = "q", required = false) String q,
                         Model model) {
        List<ClienteResponse> clientes = (q == null || q.isBlank())
                ? clienteService.listarTodos()
                : clienteService.buscarPorTexto(q);

        model.addAttribute("clientes", clientes);
        model.addAttribute("q", q);
        model.addAttribute("titulo", "Clientes");
        model.addAttribute("tituloSeccion", "Clientes");
        model.addAttribute("seccionActiva", "clientes");
        return "clientes/list";
    }

    @GetMapping("/nuevo")
    public String formNuevo(Model model) {
        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", new ClienteCreateRequest(
                    "", "", "", "", "", "", "", "", "Espana"));
        }
        model.addAttribute("titulo", "Nuevo cliente");
        model.addAttribute("tituloSeccion", "Nuevo cliente");
        model.addAttribute("seccionActiva", "clientes");
        model.addAttribute("modoEdicion", false);
        return "clientes/form";
    }

    @PostMapping
    public String crear(@Valid @ModelAttribute("cliente") ClienteCreateRequest cliente,
                        BindingResult binding,
                        Model model,
                        RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("titulo", "Nuevo cliente");
            model.addAttribute("tituloSeccion", "Nuevo cliente");
            model.addAttribute("seccionActiva", "clientes");
            model.addAttribute("modoEdicion", false);
            return "clientes/form";
        }

        try {
            ClienteCreadoResponse creado = clienteService.crear(cliente);
            redirect.addFlashAttribute("clienteCreado", creado);
            return "redirect:/admin/clientes/" + creado.cliente().id() + "/credenciales";
        } catch (IllegalArgumentException ex) {
            binding.reject("error.cliente", ex.getMessage());
            model.addAttribute("titulo", "Nuevo cliente");
            model.addAttribute("tituloSeccion", "Nuevo cliente");
            model.addAttribute("seccionActiva", "clientes");
            model.addAttribute("modoEdicion", false);
            return "clientes/form";
        }
    }

    @GetMapping("/{id}/credenciales")
    public String credenciales(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("clienteCreado")) {
            return "redirect:/admin/clientes/" + id;
        }
        model.addAttribute("titulo", "Credenciales del nuevo cliente");
        model.addAttribute("tituloSeccion", "Credenciales del nuevo cliente");
        model.addAttribute("seccionActiva", "clientes");
        return "clientes/credenciales";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        ClienteResponse cliente = clienteService.obtenerPorId(id);
        model.addAttribute("cliente", cliente);
        model.addAttribute("titulo", "Cliente: " + cliente.nombreCompleto());
        model.addAttribute("tituloSeccion", "Detalle de cliente");
        model.addAttribute("seccionActiva", "clientes");
        return "clientes/detalle";
    }

    @GetMapping("/{id}/editar")
    public String formEditar(@PathVariable Long id, Model model) {
        ClienteResponse actual = clienteService.obtenerPorId(id);
        if (!model.containsAttribute("cliente")) {
            ClienteUpdateRequest editForm = new ClienteUpdateRequest(
                    actual.nombre(),
                    actual.apellidos(),
                    actual.nif(),
                    actual.telefono(),
                    actual.direccion(),
                    actual.ciudad(),
                    actual.codigoPostal(),
                    actual.pais(),
                    actual.activo()
            );
            model.addAttribute("cliente", editForm);
        }
        model.addAttribute("clienteId", id);
        model.addAttribute("emailCliente", actual.email());
        model.addAttribute("titulo", "Editar cliente");
        model.addAttribute("tituloSeccion", "Editar cliente");
        model.addAttribute("seccionActiva", "clientes");
        model.addAttribute("modoEdicion", true);
        return "clientes/form";
    }

    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("cliente") ClienteUpdateRequest cliente,
                             BindingResult binding,
                             Model model,
                             RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            ClienteResponse actual = clienteService.obtenerPorId(id);
            model.addAttribute("clienteId", id);
            model.addAttribute("emailCliente", actual.email());
            model.addAttribute("titulo", "Editar cliente");
            model.addAttribute("tituloSeccion", "Editar cliente");
            model.addAttribute("seccionActiva", "clientes");
            model.addAttribute("modoEdicion", true);
            return "clientes/form";
        }

        clienteService.actualizar(id, cliente);
        redirect.addFlashAttribute("flashOk", "Cliente actualizado correctamente.");
        return "redirect:/admin/clientes/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            clienteService.eliminar(id);
            redirect.addFlashAttribute("flashOk", "Cliente eliminado correctamente.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("flashError",
                    "No se ha podido eliminar el cliente: " + ex.getMessage());
        }
        return "redirect:/admin/clientes";
    }
}