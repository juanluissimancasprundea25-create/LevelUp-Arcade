package com.leveluparcade.service.impl;

import com.leveluparcade.entity.AuditoriaLog;
import com.leveluparcade.repository.AuditoriaLogRepository;
import com.leveluparcade.service.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AuditoriaServiceImpl implements AuditoriaService {

    private final AuditoriaLogRepository repository;

    public AuditoriaServiceImpl(AuditoriaLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<AuditoriaLog> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public Page<AuditoriaLog> filtrarPorUsuario(Long usuarioId, Pageable pageable) {
        return repository.findByUsuarioId(usuarioId, pageable);
    }

    @Override
    public Page<AuditoriaLog> filtrarPorAccion(String accion, Pageable pageable) {
        return repository.findByAccion(accion, pageable);
    }

    @Override
    public Page<AuditoriaLog> filtrarPorEntidad(String entidad, Pageable pageable) {
        return repository.findByEntidad(entidad, pageable);
    }

    @Override
    public Page<AuditoriaLog> filtrarPorRangoFechas(
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        return repository.findByFechaBetween(desde, hasta, pageable);
    }
}