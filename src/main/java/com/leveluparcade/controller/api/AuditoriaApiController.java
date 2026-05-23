package com.leveluparcade.controller.api;

import com.leveluparcade.entity.AuditoriaLog;
import com.leveluparcade.service.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * Consulta del log de auditoria desde el panel admin.
 *
 * <p>Los resultados estan paginados (default: 20 por pagina,
 * ordenados por fecha descendente).
 */
@RestController
@RequestMapping("/api/auditoria")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaApiController {

    private final AuditoriaService auditoriaService;

    public AuditoriaApiController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public Page<AuditoriaLog> listar(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) LocalDateTime desde,
            @RequestParam(required = false) LocalDateTime hasta,
            @PageableDefault(size = 20, sort = "fecha",
                direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {

        if (usuarioId != null) {
            return auditoriaService.filtrarPorUsuario(usuarioId, pageable);
        }
        if (accion != null && !accion.isBlank()) {
            return auditoriaService.filtrarPorAccion(accion, pageable);
        }
        if (entidad != null && !entidad.isBlank()) {
            return auditoriaService.filtrarPorEntidad(entidad, pageable);
        }
        if (desde != null && hasta != null) {
            return auditoriaService.filtrarPorRangoFechas(desde, hasta, pageable);
        }
        return auditoriaService.listar(pageable);
    }
}