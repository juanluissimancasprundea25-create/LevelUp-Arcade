package com.leveluparcade.controller.web;

import com.leveluparcade.entity.Categoria;
import com.leveluparcade.entity.Producto;
import com.leveluparcade.entity.Proveedor;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.CategoriaRepository;
import com.leveluparcade.repository.ProductoRepository;
import com.leveluparcade.repository.ProveedorRepository;
import com.leveluparcade.service.ImagenProductoService;
import com.leveluparcade.service.ProductoService;
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
import org.springframework.web.multipart.MultipartFile;
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
 *
 * <p>PR #27: la imagen del producto ya NO se introduce por URL. Se sube
 * un fichero (JPG/PNG/WEBP) que el {@link ImagenProductoService} recorta
 * a 1:1, redimensiona a 600x600 y guarda en disco como JPEG. En BD se
 * guarda el path relativo (ejemplo: {@code productos/abc123.jpg}) en la
 * columna {@code imagen_url} (nombre conservado por compatibilidad con
 * datos existentes que aun pueden ser URLs externas).
 */
@Controller
@RequestMapping("/admin/productos")
@PreAuthorize("hasRole('ADMIN')")
public class ProductoWebController {

    private static final String SECCION = "productos";

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ImagenProductoService imagenProductoService;
    private final ProductoService productoService;

    public ProductoWebController(ProductoRepository productoRepository,
                                 CategoriaRepository categoriaRepository,
                                 ProveedorRepository proveedorRepository,
                                 ImagenProductoService imagenProductoService,
                                 ProductoService productoService) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.imagenProductoService = imagenProductoService;
        this.productoService = productoService;
    }

    /** Listado con filtros opcionales por texto, categoria y bajo stock. */
    @GetMapping
    public String listar(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "bajoStock", required = false) Boolean bajoStock,
            Model model) {

        List<Producto> productos = productoRepository.findAll();

        // Solo oculta del listado los productos "eliminados" (soft delete):
        // los identificamos porque al borrarlos les renombramos el SKU a
        // "BORRADO-{id}-{timestamp}". Los productos que el admin haya
        // marcado como inactivos manualmente (editando) siguen visibles
        // aqui para poder reactivarlos.
        productos = productos.stream()
                .filter(p -> p.getSku() == null || !p.getSku().startsWith("BORRADO-"))
                .toList();

        if (q != null && !q.isBlank()) {
            String filtro = q.toLowerCase().trim();
            productos = productos.stream()
                    .filter(p ->
                            (p.getNombre() != null && p.getNombre().toLowerCase().contains(filtro))
                         || (p.getSku() != null && p.getSku().toLowerCase().contains(filtro))
                         || (p.getDescripcion() != null && p.getDescripcion().toLowerCase().contains(filtro)))
                    .toList();
        }

        if (categoriaId != null) {
            productos = productos.stream()
                    .filter(p -> p.getCategoria() != null
                              && categoriaId.equals(p.getCategoria().getId()))
                    .toList();
        }

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
            @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
            RedirectAttributes ra,
            Model model) {

        if (form.getSku() != null && !form.getSku().isBlank()
                && productoRepository.existsBySku(form.getSku())) {
            bindingResult.rejectValue("sku", "duplicado",
                    "Ya existe un producto con el SKU: " + form.getSku());
        }

        // Procesar imagen si viene una nueva
        String imagenPath = null;
        if (imagenFile != null && !imagenFile.isEmpty()) {
            try {
                imagenPath = imagenProductoService.guardar(imagenFile);
            } catch (IllegalArgumentException ex) {
                bindingResult.reject("imagen", ex.getMessage());
            }
        }

        if (bindingResult.hasErrors()) {
            cargarOpcionesForm(model);
            model.addAttribute("modoEdicion", false);
            model.addAttribute("seccionActiva", SECCION);
            return "productos/form";
        }

        if (imagenPath != null) {
            form.setImagenUrl(imagenPath);
        } else {
            // Si no se sube nada, no guardar nada raro
            form.setImagenUrl(null);
        }

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
            @RequestParam(value = "imagenFile", required = false) MultipartFile imagenFile,
            @RequestParam(value = "eliminarImagen", required = false) Boolean eliminarImagen,
            RedirectAttributes ra,
            Model model) {

        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        if (form.getSku() != null && !form.getSku().equals(existente.getSku())
                && productoRepository.existsBySku(form.getSku())) {
            bindingResult.rejectValue("sku", "duplicado",
                    "Ya existe otro producto con el SKU: " + form.getSku());
        }

        // Procesar imagen si viene una nueva
        String nuevaImagen = null;
        if (imagenFile != null && !imagenFile.isEmpty()) {
            try {
                nuevaImagen = imagenProductoService.guardar(imagenFile);
            } catch (IllegalArgumentException ex) {
                bindingResult.reject("imagen", ex.getMessage());
            }
        }

        if (bindingResult.hasErrors()) {
            // Si subio una imagen valida pero hay otros errores, la
            // descartamos para evitar imagenes huerfanas.
            if (nuevaImagen != null) {
                imagenProductoService.borrarSiExiste(nuevaImagen);
            }
            // Restaurar imagenUrl del existente para que el preview del
            // form siga mostrando la imagen actual (sin esto, el rebote
            // por error de validacion la "perderia" visualmente).
            form.setImagenUrl(existente.getImagenUrl());
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
        existente.setActivo(form.getActivo() != null ? form.getActivo() : true);
        asignarCategoriaYProveedor(existente, categoriaIdForm, proveedorIdForm);

        // Gestion de imagen:
        // - Si llega nueva imagen -> reemplazar y borrar la anterior.
        // - Si marca "eliminarImagen" -> borrar la actual sin sustituto.
        // - Si no llega nada -> conservar la actual.
        String imagenAnterior = existente.getImagenUrl();
        if (nuevaImagen != null) {
            existente.setImagenUrl(nuevaImagen);
            imagenProductoService.borrarSiExiste(imagenAnterior);
        } else if (Boolean.TRUE.equals(eliminarImagen)) {
            existente.setImagenUrl(null);
            imagenProductoService.borrarSiExiste(imagenAnterior);
        }

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
        String imagen = producto.getImagenUrl();
        String nombre = producto.getNombre();

        try {
            productoService.eliminar(id);
        } catch (Exception ex) {
            ra.addFlashAttribute("flashError",
                    "No se ha podido eliminar el producto: " + ex.getMessage());
            return "redirect:/admin/productos/" + id;
        }

        // Si la entidad ya no existe -> fue un borrado real -> limpiamos
        // la imagen de disco. Si sigue existiendo es un soft delete y la
        // imagen se conserva para que la sigan viendo los pedidos historicos.
        boolean fueHardDelete = !productoRepository.existsById(id);
        if (fueHardDelete) {
            imagenProductoService.borrarSiExiste(imagen);
            ra.addFlashAttribute("flashOk",
                    "Producto eliminado: " + nombre + ".");
        } else {
            ra.addFlashAttribute("flashOk",
                    "Producto eliminado: " + nombre
                    + ". Se ha conservado el registro porque tiene pedidos asociados.");
        }
        return "redirect:/admin/productos";
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