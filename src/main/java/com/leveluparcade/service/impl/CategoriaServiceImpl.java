package com.leveluparcade.service.impl;

import com.leveluparcade.auditoria.AuditoriaEvent;
import com.leveluparcade.auditoria.AuditoriaPublisher;
import com.leveluparcade.auditoria.TipoEvento;
import com.leveluparcade.dto.request.CategoriaCreateRequest;
import com.leveluparcade.dto.request.CategoriaUpdateRequest;
import com.leveluparcade.dto.response.CategoriaResponse;
import com.leveluparcade.entity.Categoria;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.CategoriaRepository;
import com.leveluparcade.repository.ProductoRepository;
import com.leveluparcade.security.SecurityHelper;
import com.leveluparcade.service.CategoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion de {@link CategoriaService}.
 *
 * <p>El borrado de una categoria con productos asociados se rechaza a
 * nivel aplicacion (HTTP 409). A nivel BD existe {@code ON DELETE SET NULL}
 * en {@code productos.categoria_id} como red de seguridad ante DELETEs
 * directos.
 *
 * <p>Cada operacion de escritura publica un evento de auditoria.
 */
@Service
public class CategoriaServiceImpl implements CategoriaService {

    private static final Logger log = LoggerFactory.getLogger(CategoriaServiceImpl.class);

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final AuditoriaPublisher auditoria;
    private final SecurityHelper securityHelper;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository,
                                ProductoRepository productoRepository,
                                AuditoriaPublisher auditoria,
                                SecurityHelper securityHelper) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.auditoria = auditoria;
        this.securityHelper = securityHelper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll().stream()
                .map(CategoriaResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> buscarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarTodas();
        }
        return categoriaRepository.buscarPorTexto(texto.trim()).stream()
                .map(CategoriaResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(Long id) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", id));
        return CategoriaResponse.from(c);
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaCreateRequest req) {

        if (categoriaRepository.existsByNombre(req.nombre())) {
            throw new IllegalArgumentException(
                "Ya existe una categoria con el nombre: " + req.nombre());
        }

        Categoria categoria = Categoria.builder()
                .nombre(req.nombre())
                .descripcion(nullSiVacio(req.descripcion()))
                .activa(true)
                .build();

        Categoria guardada = categoriaRepository.save(categoria);
        log.info("Categoria creada: id={}, nombre={}", guardada.getId(), req.nombre());

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.CATEGORIA_CREADA, "Categoria",
            guardada.getId(), securityHelper.getUsuarioActualId(),
            "Alta de categoria: " + req.nombre()));

        return CategoriaResponse.from(guardada);
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaUpdateRequest req) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", id));

        if (!req.nombre().equals(c.getNombre())
                && categoriaRepository.existsByNombre(req.nombre())) {
            throw new IllegalArgumentException(
                "Ya existe otra categoria con el nombre: " + req.nombre());
        }

        c.setNombre(req.nombre());
        c.setDescripcion(nullSiVacio(req.descripcion()));
        if (req.activa() != null) {
            c.setActiva(req.activa());
        }

        Categoria actualizada = categoriaRepository.save(c);
        log.info("Categoria actualizada: id={}", id);

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.CATEGORIA_ACTUALIZADA, "Categoria",
            id, securityHelper.getUsuarioActualId(),
            "Actualizacion de categoria id=" + id));

        return CategoriaResponse.from(actualizada);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Categoria c = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria", id));

        long productosAsociados = productoRepository.countByCategoriaId(id);
        if (productosAsociados > 0) {
            throw new IllegalStateException(
                "No se puede eliminar la categoria '" + c.getNombre() +
                "' porque tiene " + productosAsociados + " producto(s) asociado(s). " +
                "Desactivela o reasigne los productos primero.");
        }

        String nombreEliminada = c.getNombre();
        categoriaRepository.delete(c);
        log.info("Categoria eliminada: id={}", id);

        auditoria.publish(AuditoriaEvent.entidad(
            TipoEvento.CATEGORIA_ELIMINADA, "Categoria",
            id, securityHelper.getUsuarioActualId(),
            "Eliminacion de categoria: " + nombreEliminada));
    }

    private String nullSiVacio(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}