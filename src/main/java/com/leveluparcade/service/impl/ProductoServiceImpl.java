package com.leveluparcade.service.impl;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.dto.request.AjusteStockRequest;
import com.leveluparcade.dto.request.ProductoCreateRequest;
import com.leveluparcade.dto.request.ProductoUpdateRequest;
import com.leveluparcade.dto.response.ProductoResponse;
import com.leveluparcade.entity.Categoria;
import com.leveluparcade.entity.Producto;
import com.leveluparcade.entity.Proveedor;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.CategoriaRepository;
import com.leveluparcade.repository.LineaPedidoRepository;
import com.leveluparcade.repository.ProductoRepository;
import com.leveluparcade.repository.ProveedorRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion de {@link ProductoService}.
 *
 * <p>El borrado de un producto referenciado por lineas de pedido se
 * rechaza a nivel aplicacion (HTTP 409). La FK en {@code lineas_pedido}
 * mantiene integridad referencial estricta.
 *
 * <p>Cada operacion de escritura publica un evento de auditoria.
 */
@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final LineaPedidoRepository lineaPedidoRepository;
    private final AuditoriaPublisher auditoria;
    private final SecurityHelper securityHelper;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               ProveedorRepository proveedorRepository,
                               LineaPedidoRepository lineaPedidoRepository,
                               AuditoriaPublisher auditoria,
                               SecurityHelper securityHelper) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.proveedorRepository = proveedorRepository;
        this.lineaPedidoRepository = lineaPedidoRepository;
        this.auditoria = auditoria;
        this.securityHelper = securityHelper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarTodos() {
        return productoRepository.findAll().stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> buscarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }
        return productoRepository.buscarPorTexto(texto.trim()).stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPorCategoria(Long categoriaId) {
        if (!categoriaRepository.existsById(categoriaId)) {
            throw new ResourceNotFoundException("Categoria", categoriaId);
        }
        return productoRepository.findByCategoriaId(categoriaId).stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPorProveedor(Long proveedorId) {
        if (!proveedorRepository.existsById(proveedorId)) {
            throw new ResourceNotFoundException("Proveedor", proveedorId);
        }
        return productoRepository.findByProveedorId(proveedorId).stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarBajoStock() {
        return productoRepository.findBajoStock().stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
        return ProductoResponse.from(p);
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoCreateRequest req) {

        if (productoRepository.existsBySku(req.sku())) {
            throw new IllegalArgumentException(
                "Ya existe un producto con el SKU: " + req.sku());
        }

        Categoria categoria = resolverCategoria(req.categoriaId());
        Proveedor proveedor = resolverProveedor(req.proveedorId());

        Producto producto = Producto.builder()
                .sku(req.sku().toUpperCase())
                .nombre(req.nombre())
                .descripcion(nullSiVacio(req.descripcion()))
                .precio(req.precio())
                .stock(req.stock())
                .stockMinimo(req.stockMinimo() != null ? req.stockMinimo() : 5)
                .categoria(categoria)
                .proveedor(proveedor)
                .imagenUrl(nullSiVacio(req.imagenUrl()))
                .activo(true)
                .build();

        Producto guardado = productoRepository.save(producto);
        log.info("Producto creado: id={}, sku={}", guardado.getId(), req.sku());

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.PRODUCTO_CREADO, "Producto",
            guardado.getId(), securityHelper.getUsuarioActualId(),
            "Alta de producto: " + req.nombre() + " (SKU " + req.sku() + ")"));

        return ProductoResponse.from(guardado);
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoUpdateRequest req) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        String skuNormalizado = req.sku().toUpperCase();
        if (!skuNormalizado.equals(p.getSku())
                && productoRepository.existsBySku(skuNormalizado)) {
            throw new IllegalArgumentException(
                "Ya existe otro producto con el SKU: " + skuNormalizado);
        }

        Categoria categoria = resolverCategoria(req.categoriaId());
        Proveedor proveedor = resolverProveedor(req.proveedorId());

        p.setSku(skuNormalizado);
        p.setNombre(req.nombre());
        p.setDescripcion(nullSiVacio(req.descripcion()));
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        if (req.stockMinimo() != null) {
            p.setStockMinimo(req.stockMinimo());
        }
        p.setCategoria(categoria);
        p.setProveedor(proveedor);
        p.setImagenUrl(nullSiVacio(req.imagenUrl()));
        if (req.activo() != null) {
            p.setActivo(req.activo());
        }

        Producto actualizado = productoRepository.save(p);
        log.info("Producto actualizado: id={}", id);

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.PRODUCTO_ACTUALIZADO, "Producto",
            id, securityHelper.getUsuarioActualId(),
            "Actualizacion de producto id=" + id));

        return ProductoResponse.from(actualizado);
    }

    @Override
    @Transactional
    public ProductoResponse ajustarStock(Long id, AjusteStockRequest req) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        int stockActual = p.getStock();
        int nuevoStock = stockActual + req.cantidad();
        if (nuevoStock < 0) {
            throw new IllegalStateException(
                "El ajuste dejaria el stock en negativo. Stock actual: " +
                stockActual + ", ajuste: " + req.cantidad());
        }

        p.setStock(nuevoStock);
        Producto actualizado = productoRepository.save(p);
        log.info("Stock ajustado: producto id={}, {} -> {} ({}{})",
                id, stockActual, nuevoStock,
                req.cantidad() >= 0 ? "+" : "", req.cantidad());

        String motivo = (req.motivo() == null || req.motivo().isBlank())
                ? "Ajuste manual"
                : req.motivo();

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.PRODUCTO_STOCK_AJUSTADO, "Producto",
            id, securityHelper.getUsuarioActualId(),
            "Ajuste de stock " + stockActual + " -> " + nuevoStock +
            " (" + (req.cantidad() >= 0 ? "+" : "") + req.cantidad() +
            "). Motivo: " + motivo));

        return ProductoResponse.from(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto p = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));

        String nombreEliminado = p.getNombre();
        String skuOriginal = p.getSku();

        // Si el producto NO esta referenciado por ninguna linea de pedido
        // podemos borrarlo de verdad. En caso contrario, se hace "soft
        // delete": se libera el SKU (renombrandolo) y se marca como
        // inactivo. Asi se preserva la integridad del historial y a la vez
        // el admin puede dar de alta otro producto con el mismo SKU.
        long lineas = lineaPedidoRepository.countByProductoId(id);

        if (lineas == 0) {
            productoRepository.delete(p);
            productoRepository.flush();
            log.info("Producto eliminado (hard): id={}", id);
            auditoria.publish(AuditoriaEvent.entidad(
                TipoEvento.PRODUCTO_ELIMINADO, "Producto",
                id, securityHelper.getUsuarioActualId(),
                "Eliminacion de producto: " + nombreEliminado
                        + " (SKU " + skuOriginal + ")"));
            return;
        }

        // Soft delete: hay pedidos historicos referenciandolo.
        p.setActivo(false);
        p.setSku("BORRADO-" + id + "-" + System.currentTimeMillis());
        productoRepository.save(p);

        log.info("Producto eliminado (soft, {} lineas referencian): id={}", lineas, id);
        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.PRODUCTO_ELIMINADO, "Producto",
            id, securityHelper.getUsuarioActualId(),
            "Producto desactivado (tenia " + lineas + " linea(s) de pedido): "
                    + nombreEliminado + " (SKU original " + skuOriginal + ")"));
    }

    private Categoria resolverCategoria(Long categoriaId) {
        if (categoriaId == null) return null;
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", categoriaId));
    }

    private Proveedor resolverProveedor(Long proveedorId) {
        if (proveedorId == null) return null;
        return proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", proveedorId));
    }

    private String nullSiVacio(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}