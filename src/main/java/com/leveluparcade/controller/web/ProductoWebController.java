package com.leveluparcade.controller.web;

import com.leveluparcade.entity.Categoria;
import com.leveluparcade.entity.Producto;
import com.leveluparcade.entity.Proveedor;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.CategoriaRepository;
import com.leveluparcade.repository.ProductoRepository;
import com.leveluparcade.repository.ProveedorRepository;
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

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller web para gestion de productos desde el panel admin.
 *
 * <p>Trabaja directamente con la entidad {@code Producto} y los
 * repositorios (no via service) porque el ProductoService de Ivan
 * todavia no expone los metodos de busqueda/filtrado que esta vista
 * necesita. Si en el futuro Ivan amplia el service con DTOs, esta vista
 * se puede refactorizar para usarlos.
 *
 * <p>Acordado con Ivan (autor del modulo de Productos).
 */
@Controller
@RequestMapping("/admin/productos")
@PreAuthorize("hasRole('ADMIN')")
public class ProductoWebController {

    private static final String SECCION = "productos";

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;

    public ProductoWebController(ProductoRepository productoRepository,
                                 CategoriaRepository categoriaRepository,
                                 ProveedorRepository proveedorRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
    }

    /** Listado con filtros opcionales por texto, categoria y bajo stock. */
    @GetMapping
    public String listar(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "bajoStock", required = false) Boolean bajoStock,
            Model model) {

        List<Producto> productos = productoRepository.findAll();

        // Filtro por texto (nombre, SKU, descripcion)
        if (q != null && !q.isBlank()) {
            String filtro = q.toLowerCase().trim();
            productos = productos.stream()
                    .filter(p ->
                            (p.getNombre() != null && p.getNombre().toLowerCase().contains(filtro))
                         || (p.getSku() != null && p.getSku().toLowerCase().contains(filtro))
                         || (p.getDescripcion() != null && p.getDescripcion().toLowerCase().contains(filtro)))
                    .toList();
        }

        // Filtro por categoria
        if (categoriaId != null) {
            productos = productos.stream()
                    .filter(p -> p.getCategoria() != null
                              && categoriaId.equals(p.getCategoria().getId()))
                    .toList();
        }

        // Filtro por bajo stock
        if (Boolean.TRUE.equals(bajoStock)) {
            productos = productos.stream()
                    .filter(p -> p.getStock() != null
                              && p.getStockMinimo() != null
                              && p.getStock() <= p.getStockMinimo())
                    .toList();
        }

        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("q", q);
        model.addAttribute("categoriaIdFiltro", categoriaId);
        model.addAttribute("bajoStockFiltro", bajoStock);
        model.addAttribute("seccionActiva", SECCION);
        return "productos/list";
    }

    /** Detalle de un producto. */
    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
        model.addAttribute("producto", producto);
        model.addAttribute("seccionActiva", SECCION);
        return "productos/detalle";
    }

    /** Formulario de alta. */
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        if (!model.containsAttribute("productoForm")) {
            Producto vacio = new Producto();
            vacio.setActivo(true);
            vacio.setStock(0);
            vacio.setStockMinimo(5);
            vacio.setPrecio(BigDecimal.ZERO);
            model.addAttribute("productoForm", vacio);
        }
        cargarOpcionesForm(model);
        model.addAttribute("modoEdicion", false);
        model.addAttribute("seccionActiva", SECCION);
        return "productos/form";
    }

    /** Procesa el alta. */
    @PostMapping
    public String crear(
            @Valid @ModelAttribute("productoForm") Producto form,
            BindingResult bindingResult,
            @RequestParam(value = "categoriaIdForm", required = false) Long categoriaIdForm,
            @RequestParam(value = "proveedorIdForm", required = false) Long proveedorIdForm,
            RedirectAttributes ra,
            Model model) {

        // Validar SKU duplicado a nivel controller
        if (form.getSku() != null && !form.getSku().isBlank()
                && productoRepository.existsBySku(form.getSku())) {
            bindingResult.rejectValue("sku", "duplicado",
                    "Ya existe un producto con el SKU: " + form.getSku());
        }

        if (bindingResult.hasErrors()) {
            cargarOpcionesForm(model);
            model.addAttribute("modoEdicion", false);
            model.addAttribute("seccionActiva", SECCION);
            return "productos/form";
        }

        // Asignar relaciones manualmente desde los IDs
        asignarCategoriaYProveedor(form, categoriaIdForm, proveedorIdForm);

        Producto guardado = productoRepository.save(form);
        ra.addFlashAttribute("flashOk",
                "Producto '" + guardado.getNombre() + "' creado correctamente.");
        return "redirect:/admin/productos/" + guardado.getId();
    }

    /** Formulario de edicion. */
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        if (!model.containsAttribute("productoForm")) {
            model.addAttribute("productoForm", producto);
        }
        cargarOpcionesForm(model);
        model.addAttribute("productoId", id);
        model.addAttribute("nombreOriginal", producto.getNombre());
        model.addAttribute("skuOriginal", producto.getSku());
        model.addAttribute("modoEdicion", true);
        model.addAttribute("seccionActiva", SECCION);
        return "productos/form";
    }

    /** Procesa la edicion. */
    @PostMapping("/{id}")
    public String actualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("productoForm") Producto form,
            BindingResult bindingResult,
            @RequestParam(value = "categoriaIdForm", required = false) Long categoriaIdForm,
            @RequestParam(value = "proveedorIdForm", required = false) Long proveedorIdForm,
            RedirectAttributes ra,
            Model model) {

        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        // Validar SKU duplicado solo si ha cambiado
        if (form.getSku() != null && !form.getSku().equals(existente.getSku())
                && productoRepository.existsBySku(form.getSku())) {
            bindingResult.rejectValue("sku", "duplicado",
                    "Ya existe otro producto con el SKU: " + form.getSku());
        }

        if (bindingResult.hasErrors()) {
            cargarOpcionesForm(model);
            model.addAttribute("productoId", id);
            model.addAttribute("modoEdicion", true);
            model.addAttribute("seccionActiva", SECCION);
            return "productos/form";
        }

        // Actualizar campos sobre el existente para no perder timestamps
        existente.setSku(form.getSku());
        existente.setNombre(form.getNombre());
        existente.setDescripcion(form.getDescripcion());
        existente.setPrecio(form.getPrecio());
        existente.setStock(form.getStock());
        existente.setStockMinimo(form.getStockMinimo());
        existente.setImagenUrl(form.getImagenUrl());
        existente.setActivo(form.getActivo() != null ? form.getActivo() : true);
        asignarCategoriaYProveedor(existente, categoriaIdForm, proveedorIdForm);

        productoRepository.save(existente);
        ra.addFlashAttribute("flashOk",
                "Producto '" + existente.getNombre() + "' actualizado.");
        return "redirect:/admin/productos/" + id;
    }

    /** Eliminar. */
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
        try {
            productoRepository.delete(producto);
            ra.addFlashAttribute("flashOk", "Producto eliminado.");
            return "redirect:/admin/productos";
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError",
                    "No se puede eliminar el producto: puede tener pedidos asociados. " +
                    "Desactivelo en su lugar.");
            return "redirect:/admin/productos/" + id;
        }
    }

    // ---------- helpers ----------

    private void cargarOpcionesForm(Model model) {
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("proveedores", proveedorRepository.findAll());
    }

    private void asignarCategoriaYProveedor(Producto producto,
                                            Long categoriaId,
                                            Long proveedorId) {
        if (categoriaId != null) {
            Categoria cat = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Categoria", categoriaId));
            producto.setCategoria(cat);
        } else {
            producto.setCategoria(null);
        }

        if (proveedorId != null) {
            Proveedor prov = proveedorRepository.findById(proveedorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor", proveedorId));
            producto.setProveedor(prov);
        } else {
            producto.setProveedor(null);
        }
    }
}