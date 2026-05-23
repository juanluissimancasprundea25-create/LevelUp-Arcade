package com.leveluparcade.service;

import com.leveluparcade.entity.AuditoriaLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Servicio de consulta del log de auditoria.
 *
 * <p>El log NO se escribe desde aqui: lo hace {@code AuditoriaListener}
 * al recibir eventos. Este servicio solo expone consultas para el
 * panel admin.
 */
public interface AuditoriaService {

    Page<AuditoriaLog> listar(Pageable pageable);

    Page<AuditoriaLog> filtrarPorUsuario(Long usuarioId, Pageable pageable);

    Page<AuditoriaLog> filtrarPorAccion(String accion, Pageable pageable);

    Page<AuditoriaLog> filtrarPorEntidad(String entidad, Pageable pageable);

    Page<AuditoriaLog> filtrarPorRangoFechas(
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}