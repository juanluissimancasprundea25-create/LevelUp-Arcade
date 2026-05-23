package com.leveluparcade.service.impl;

import com.leveluparcade.dto.request.ProveedorCreateRequest;
import com.leveluparcade.dto.request.ProveedorUpdateRequest;
import com.leveluparcade.dto.response.ProveedorResponse;
import com.leveluparcade.entity.Proveedor;
import com.leveluparcade.exception.ResourceNotFoundException;
import com.leveluparcade.repository.ProductoRepository;
import com.leveluparcade.repository.ProveedorRepository;
import com.leveluparcade.service.ProveedorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementacion de {@link ProveedorService}.
 *
 * <p>El borrado de un proveedor esta protegido a nivel aplicacion:
 * si tiene productos asociados, se lanza {@code IllegalStateException}
 * que el {@code GlobalExceptionHandler} mapea a HTTP 409 Conflict.
 *
 * <p>A nivel BD existe {@code ON DELETE SET NULL} en la FK
 * productos.proveedor_id como red de seguridad ante DELETEs directos.
 */
@Service
public class ProveedorServiceImpl implements ProveedorService {

    private static final Logger log = LoggerFactory.getLogger(ProveedorServiceImpl.class);

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;

    public ProveedorServiceImpl(ProveedorRepository proveedorRepository,
                                ProductoRepository productoRepository) {
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> listarTodos() {
        return proveedorRepository.findAll().stream()
                .map(ProveedorResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponse> buscarPorTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return listarTodos();
        }
        return proveedorRepository.buscarPorTexto(texto.trim()).stream()
                .map(ProveedorResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponse obtenerPorId(Long id) {
        Proveedor p = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));
        return ProveedorResponse.from(p);
    }

    @Override
    @Transactional
    public ProveedorResponse crear(ProveedorCreateRequest req) {

        if (proveedorRepository.existsByCif(req.cif())) {
            throw new IllegalArgumentException(
                "Ya existe un proveedor con el CIF: " + req.cif());
        }

        Proveedor proveedor = Proveedor.builder()
                .nombreEmpresa(req.nombreEmpresa())
                .cif(req.cif())
                .emailContacto(nullSiVacio(req.emailContacto()))
                .telefono(nullSiVacio(req.telefono()))
                .direccion(nullSiVacio(req.direccion()))
                .ciudad(nullSiVacio(req.ciudad()))
                .codigoPostal(nullSiVacio(req.codigoPostal()))
                .pais(req.pais() == null || req.pais().isBlank() ? "Espana" : req.pais())
                .activo(true)
                .build();

        Proveedor guardado = proveedorRepository.save(proveedor);
        log.info("Proveedor creado: id={}, cif={}", guardado.getId(), req.cif());

        return ProveedorResponse.from(guardado);
    }

    @Override
    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorUpdateRequest req) {
        Proveedor p = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));

        // Validar CIF unico si cambia
        if (!req.cif().equals(p.getCif())
                && proveedorRepository.existsByCif(req.cif())) {
            throw new IllegalArgumentException(
                "Ya existe otro proveedor con el CIF: " + req.cif());
        }

        p.setNombreEmpresa(req.nombreEmpresa());
        p.setCif(req.cif());
        p.setEmailContacto(nullSiVacio(req.emailContacto()));
        p.setTelefono(nullSiVacio(req.telefono()));
        p.setDireccion(nullSiVacio(req.direccion()));
        p.setCiudad(nullSiVacio(req.ciudad()));
        p.setCodigoPostal(nullSiVacio(req.codigoPostal()));
        if (req.pais() != null && !req.pais().isBlank()) {
            p.setPais(req.pais());
        }
        if (req.activo() != null) {
            p.setActivo(req.activo());
        }

        Proveedor actualizado = proveedorRepository.save(p);
        log.info("Proveedor actualizado: id={}", id);

        return ProveedorResponse.from(actualizado);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Proveedor p = proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor", id));

        long productosAsociados = productoRepository.countByProveedorId(id);
        if (productosAsociados > 0) {
            throw new IllegalStateException(
                "No se puede eliminar el proveedor '" + p.getNombreEmpresa() +
                "' porque tiene " + productosAsociados + " producto(s) asociado(s). " +
                "Desactivelo o reasigne los productos primero.");
        }

        proveedorRepository.delete(p);
        log.info("Proveedor eliminado: id={}", id);
    }

    private String nullSiVacio(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}