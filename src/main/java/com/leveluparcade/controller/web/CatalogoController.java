package com.leveluparcade.controller.web;

import com.leveluparcade.entity.Producto;
import com.leveluparcade.repository.CategoriaRepository;
import com.leveluparcade.repository.ProductoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador del catalogo publico de la tienda.
 *
 * <p>Endpoints publicos (cadena 4 de SecurityConfig):
 * <ul>
 *   <li>{@code GET /catalogo} - listado paginado con filtros (q, categoria, orden).</li>
 *   <li>{@code GET /producto/{id}} - detalle de un producto.</li>
 * </ul>
 *
 * <p>Lee directamente del {@link ProductoRepository} (acordado con Ivan,
 * patron "Opcion A" para Productos: vistas web -> repositorio).
 */
@Controller
public class CatalogoController {

    /** Tamano de pagina del catalogo. */
    private static final int TAMANO_PAGINA = 12;

    /** Ordenes permitidos en el selector del catalogo. */
    private static final List<String> ORDENES_PERMITIDOS =
            List.of("novedad", "precio_asc", "precio_desc", "nombre");

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public CatalogoController(ProductoRepository productoRepository,
                              CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * Catalogo publico paginado.
     *
     * @param q          texto de busqueda libre sobre nombre/SKU/descripcion
     * @param categoria  id de categoria para filtrar (opcional)
     * @param orden      clave de orden: novedad | precio_asc | precio_desc | nombre
     * @param page       numero de pagina (0-based)
     */
    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(value = "q", required = false) String q,
                           @RequestParam(value = "categoria", required = false) Long categoria,
                           @RequestParam(value = "orden", required = false, defaultValue = "novedad") String orden,
                           @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                           Model model) {

        // Sanear orden por si llega algo raro por querystring
        String ordenSeguro = ORDENES_PERMITIDOS.contains(orden) ? orden : "novedad";
        int paginaSegura = Math.max(0, page);

        Pageable pageable = PageRequest.of(paginaSegura, TAMANO_PAGINA, sortFromOrden(ordenSeguro));
        Specification<Producto> spec = buildSpec(q, categoria);

        Page<Producto> resultado = productoRepository.findAll(spec, pageable);

        model.addAttribute("titulo", "Catalogo");
        model.addAttribute("productos", resultado.getContent());
        model.addAttribute("paginaActual", resultado.getNumber());
        model.addAttribute("totalPaginas", resultado.getTotalPages());
        model.addAttribute("totalProductos", resultado.getTotalElements());
        model.addAttribute("categorias", categoriaRepository.findByActivaTrue());
        model.addAttribute("q", q);
        model.addAttribute("categoriaSeleccionada", categoria);
        model.addAttribute("orden", ordenSeguro);
        return "tienda/catalogo";
    }

    /**
     * Detalle publico de un producto. Si el id no existe o el producto
     * esta inactivo, vuelve al catalogo con un mensaje flash.
     */
    @GetMapping("/producto/{id}")
    public String detalle(@PathVariable Long id,
                          Model model,
                          RedirectAttributes flash) {

        Producto producto = productoRepository.findById(id).orElse(null);
        if (producto == null || Boolean.FALSE.equals(producto.getActivo())) {
            flash.addFlashAttribute("error", "El producto solicitado no esta disponible.");
            return "redirect:/catalogo";
        }

        // Relacionados: hasta 4 productos activos de la misma categoria,
        // distintos al actual. Lectura barata via Specification.
        List<Producto> relacionados = new ArrayList<>();
        if (producto.getCategoria() != null) {
            Long catId = producto.getCategoria().getId();
            Specification<Producto> spec = (root, query, cb) -> cb.and(
                    cb.equal(root.get("activo"), true),
                    cb.equal(root.get("categoria").get("id"), catId),
                    cb.notEqual(root.get("id"), producto.getId())
            );
            relacionados = productoRepository.findAll(spec, PageRequest.of(0, 4)).getContent();
        }

        model.addAttribute("titulo", producto.getNombre());
        model.addAttribute("producto", producto);
        model.addAttribute("relacionados", relacionados);
        return "tienda/producto";
    }

    // ---------- helpers ----------

    /**
     * Construye la Specification del catalogo con los filtros pasados.
     * Solo muestra productos {@code activo = true}.
     */
    private Specification<Producto> buildSpec(String q, Long categoriaId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("activo"), true));

            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase().trim() + "%";
                Predicate porNombre = cb.like(cb.lower(root.get("nombre")), like);
                Predicate porSku = cb.like(cb.lower(root.get("sku")), like);
                Predicate porDesc = cb.like(cb.lower(root.get("descripcion")), like);
                predicates.add(cb.or(porNombre, porSku, porDesc));
            }
            if (categoriaId != null) {
                predicates.add(cb.equal(root.get("categoria").get("id"), categoriaId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Traduce la clave de orden de la UI a un {@link Sort} de Spring Data. */
    private Sort sortFromOrden(String orden) {
        return switch (orden) {
            case "precio_asc" -> Sort.by(Sort.Direction.ASC, "precio");
            case "precio_desc" -> Sort.by(Sort.Direction.DESC, "precio");
            case "nombre" -> Sort.by(Sort.Direction.ASC, "nombre");
            default -> Sort.by(Sort.Direction.DESC, "fechaAlta"); // novedad
        };
    }
}
