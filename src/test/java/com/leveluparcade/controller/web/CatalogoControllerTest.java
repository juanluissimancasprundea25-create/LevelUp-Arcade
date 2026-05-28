package com.leveluparcade.controller.web;

import com.leveluparcade.config.SecurityConfig;
import com.leveluparcade.controller.api.WebMvcSecurityTestConfig;
import com.leveluparcade.entity.Categoria;
import com.leveluparcade.entity.Producto;
import com.leveluparcade.repository.CategoriaRepository;
import com.leveluparcade.repository.ProductoRepository;
import com.leveluparcade.security.JwtFilter;
import com.leveluparcade.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests del CatalogoController publico.
 *
 * <p>Excluye SecurityConfig real y JwtFilter para evitar arrastrar
 * la cadena de seguridad completa (mismo patron que los tests api).
 * Las rutas son publicas, no hay @PreAuthorize que probar.
 */
@WebMvcTest(
    controllers = CatalogoController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { SecurityConfig.class, JwtFilter.class, JwtService.class }
    )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebMvcSecurityTestConfig.class)
@DisplayName("CatalogoController - tests web mvc")
class CatalogoControllerTest {

    @Autowired MockMvc mvc;

    @MockBean ProductoRepository productoRepository;
    @MockBean CategoriaRepository categoriaRepository;

    // ---------- /catalogo ----------

    @Test
    @DisplayName("GET /catalogo sin filtros -> 200 y modelo poblado")
    void catalogoSinFiltros() throws Exception {
        Producto p = nuevoProducto(1L, "Mando Pro", new BigDecimal("49.99"), 10, true);
        Page<Producto> page = new PageImpl<>(List.of(p), PageRequest.of(0, 12), 1);

        when(productoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(categoriaRepository.findByActivaTrue()).thenReturn(List.of(nuevaCategoria(5L, "Mandos")));

        mvc.perform(get("/catalogo"))
            .andExpect(status().isOk())
            .andExpect(view().name("tienda/catalogo"))
            .andExpect(model().attributeExists("productos", "categorias", "paginaActual",
                                                "totalPaginas", "totalProductos", "orden"))
            .andExpect(model().attribute("orden", "novedad"))
            .andExpect(model().attribute("paginaActual", 0))
            .andExpect(model().attribute("totalPaginas", 1))
            .andExpect(model().attribute("totalProductos", 1L));
    }

    @Test
    @DisplayName("GET /catalogo con orden invalido -> se sanea a novedad")
    void catalogoOrdenInvalidoSeSanea() throws Exception {
        when(productoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(categoriaRepository.findByActivaTrue()).thenReturn(Collections.emptyList());

        mvc.perform(get("/catalogo").param("orden", "DROP_TABLE"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orden", "novedad"));
    }

    @Test
    @DisplayName("GET /catalogo con orden precio_asc -> Sort.ASC sobre precio")
    void catalogoOrdenPrecioAsc() throws Exception {
        when(productoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(categoriaRepository.findByActivaTrue()).thenReturn(Collections.emptyList());

        mvc.perform(get("/catalogo").param("orden", "precio_asc"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("orden", "precio_asc"));

        // Aprovecho para verificar que el sort llega al repo
        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(productoRepository).findAll(any(Specification.class), captor.capture());
        Sort sort = captor.getValue().getSort();
        org.assertj.core.api.Assertions.assertThat(sort.getOrderFor("precio"))
                .isNotNull()
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("GET /catalogo con page negativo -> se sanea a 0")
    void catalogoPageNegativoSeSanea() throws Exception {
        when(productoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(categoriaRepository.findByActivaTrue()).thenReturn(Collections.emptyList());

        mvc.perform(get("/catalogo").param("page", "-5"))
            .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(productoRepository).findAll(any(Specification.class), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getPageNumber()).isZero();
    }

    // ---------- /producto/{id} ----------

    @Test
    @DisplayName("GET /producto/{id} con producto activo -> 200 y vista detalle")
    void detalleProductoActivo() throws Exception {
        Producto p = nuevoProducto(42L, "Auriculares Gamer", new BigDecimal("79.50"), 3, true);
        p.setCategoria(nuevaCategoria(8L, "Audio"));

        when(productoRepository.findById(42L)).thenReturn(Optional.of(p));
        when(productoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mvc.perform(get("/producto/42"))
            .andExpect(status().isOk())
            .andExpect(view().name("tienda/producto"))
            .andExpect(model().attributeExists("producto", "relacionados"));
    }

    @Test
    @DisplayName("GET /producto/{id} inexistente -> redirect a /catalogo")
    void detalleProductoInexistente() throws Exception {
        when(productoRepository.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/producto/999"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/catalogo"));
    }

    @Test
    @DisplayName("GET /producto/{id} inactivo -> redirect a /catalogo")
    void detalleProductoInactivo() throws Exception {
        Producto p = nuevoProducto(7L, "Producto Retirado", new BigDecimal("10.00"), 5, false);
        when(productoRepository.findById(7L)).thenReturn(Optional.of(p));

        mvc.perform(get("/producto/7"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/catalogo"));
    }

    // ---------- helpers ----------

    private Producto nuevoProducto(Long id, String nombre, BigDecimal precio, int stock, boolean activo) {
        Producto p = new Producto();
        p.setId(id);
        p.setSku("SKU-" + id);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setStockMinimo(2);
        p.setActivo(activo);
        p.setFechaAlta(LocalDateTime.now());
        p.setFechaActualizacion(LocalDateTime.now());
        return p;
    }

    private Categoria nuevaCategoria(Long id, String nombre) {
        Categoria c = new Categoria();
        c.setId(id);
        c.setNombre(nombre);
        c.setActiva(true);
        return c;
    }
}
