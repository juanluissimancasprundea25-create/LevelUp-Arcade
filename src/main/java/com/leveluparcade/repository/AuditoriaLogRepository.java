package com.leveluparcade.repository;

import com.leveluparcade.entity.AuditoriaLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repositorio para la entidad {@link AuditoriaLog}.
 *
 * <p>El log de auditoria puede crecer rapidamente, por eso las consultas
 * principales devuelven {@link Page} en lugar de {@link List}: el frontend
 * pagina los resultados.
 */
@Repository
public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {

    Page<AuditoriaLog> findByUsuarioId(Long usuarioId, Pageable pageable);

    /**
     * Filtra eventos cuyos usuarios estan en el conjunto dado. Usado para
     * buscar en auditoria por nombre / email (que primero se resuelve a
     * una lista de ids).
     */
    Page<AuditoriaLog> findByUsuarioIdIn(Collection<Long> usuarioIds, Pageable pageable);

    Page<AuditoriaLog> findByAccion(String accion, Pageable pageable);

    Page<AuditoriaLog> findByEntidad(String entidad, Pageable pageable);

    Page<AuditoriaLog> findByFechaBetween(
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}