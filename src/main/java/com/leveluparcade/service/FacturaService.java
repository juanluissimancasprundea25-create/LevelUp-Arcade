package com.leveluparcade.service;

import com.leveluparcade.dto.request.EmitirFacturaRequest;
import com.leveluparcade.dto.response.FacturaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface FacturaService {

    FacturaResponse emitirFactura(EmitirFacturaRequest request);

    FacturaResponse obtenerPorId(Long id);

    Page<FacturaResponse> listarTodas(Long clienteId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<FacturaResponse> listarMias(Pageable pageable);

    byte[] descargarPdf(Long id);

    FacturaResponse verificar(String numeroFactura);
}