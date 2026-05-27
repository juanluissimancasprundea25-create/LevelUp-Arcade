package com.leveluparcade.controller.web;

import com.leveluparcade.dto.request.ProveedorCreateRequest;
import com.leveluparcade.dto.request.ProveedorUpdateRequest;
import com.leveluparcade.dto.response.ProveedorResponse;
import com.leveluparcade.service.ProveedorService;
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

/**
 * Controller web para gestion de proveedores desde el panel admin.
 *
 * <p>Llama directamente al ProveedorService (Opcion A), reutilizando la
 * misma logica de negocio que la API REST {@code /api/proveedores}.
 *
 * <p>Protegido por {@code @PreAuthorize("hasRole('ADMIN')")} + regla
 * global de SecurityConfig.
 */
@Controller
@RequestMapping("/admin/proveedores")
@PreAuthorize("hasRole('ADMIN')")
public class ProveedorWebController {

    private static final String SECCION = "proveedores";

    private final ProveedorService proveedorService;

    public ProveedorWebController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    /** Listado con buscador opcional. */
    @GetMapping
    public String listar(
            @RequestParam(value = "q", required = false) String q,
            Model model) {

        List<ProveedorResponse> proveedores = (q == null || q.isBlank())
                ? proveedorService.listarTodos()
                : proveedorService.buscarPorTexto(q);

        model.addAttribute("proveedores", proveedores);
        model.addAttribute("q", q);
        model.addAttribute("seccionActiva", SECCION);
        return "proveedores/list";
    }

    /** Detalle de un proveedor. */
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        ProveedorResponse proveedor = proveedorService.obtenerPorId(id);
        model.addAttribute("proveedor", proveedor);
        model.addAttribute("seccionActiva", SECCION);
        return "proveedores/detalle";
    }

    /** Formulario de alta. */
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        if (!model.containsAttribute("proveedorForm")) {
            model.addAttribute("proveedorForm", new ProveedorCreateRequest(
                    "", "", "", "", "", "", "", ""));
        }
        model.addAttribute("modoEdicion", false);
        model.addAttribute("seccionActiva", SECCION);
        return "proveedores/form";
    }

    /** Procesa el alta. */
    @PostMapping
    public String crear(
            @Valid @ModelAttribute("proveedorForm") ProveedorCreateRequest form,
            BindingResult bindingResult,
            RedirectAttributes ra,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("modoEdicion", false);
            model.addAttribute("seccionActiva", SECCION);
            return "proveedores/form";
        }

        try {
            ProveedorResponse creado = proveedorService.crear(form);
            ra.addFlashAttribute("flashOk",
                    "Proveedor '" + creado.nombreEmpresa() + "' creado correctamente.");
            return "redirect:/proveedores/" + creado.id();
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("cif", "duplicado", ex.getMessage());
            model.addAttribute("modoEdicion", false);
            model.addAttribute("seccionActiva", SECCION);
            return "proveedores/form";
        }
    }

    /** Formulario de edicion. */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        ProveedorResponse p = proveedorService.obtenerPorId(id);

        if (!model.containsAttribute("proveedorForm")) {
            ProveedorUpdateRequest form = new ProveedorUpdateRequest(
                    p.nombreEmpresa(),
                    p.cif(),
                    p.emailContacto() == null ? "" : p.emailContacto(),
                    p.telefono() == null ? "" : p.telefono(),
                    p.direccion() == null ? "" : p.direccion(),
                    p.ciudad() == null ? "" : p.ciudad(),
                    p.codigoPostal() == null ? "" : p.codigoPostal(),
                    p.pais() == null ? "" : p.pais(),
                    p.activo()
            );
            model.addAttribute("proveedorForm", form);
        }

        model.addAttribute("proveedorId", id);
        model.addAttribute("nombreOriginal", p.nombreEmpresa());
        model.addAttribute("modoEdicion", true);
        model.addAttribute("seccionActiva", SECCION);
        return "proveedores/form";
    }

    /** Procesa la edicion. */
    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("proveedorForm") ProveedorUpdateRequest form,
            BindingResult bindingResult,
            RedirectAttributes ra,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("proveedorId", id);
            model.addAttribute("modoEdicion", true);
            model.addAttribute("seccionActiva", SECCION);
            return "proveedores/form";
        }

        try {
            ProveedorResponse actualizado = proveedorService.actualizar(id, form);
            ra.addFlashAttribute("flashOk",
                    "Proveedor '" + actualizado.nombreEmpresa() + "' actualizado.");
            return "redirect:/proveedores/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("cif", "duplicado", ex.getMessage());
            model.addAttribute("proveedorId", id);
            model.addAttribute("modoEdicion", true);
            model.addAttribute("seccionActiva", SECCION);
            return "proveedores/form";
        }
    }

    /** Eliminar. */
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        try {
            proveedorService.eliminar(id);
            ra.addFlashAttribute("flashOk", "Proveedor eliminado.");
            return "redirect:/proveedores";
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/proveedores/" + id;
        }
    }
}