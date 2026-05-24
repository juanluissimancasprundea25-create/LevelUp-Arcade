package com.leveluparcade.service;

import com.leveluparcade.dto.request.CrearDevolucionRequest;
import com.leveluparcade.dto.request.RechazarDevolucionRequest;
import com.leveluparcade.dto.response.DevolucionResponse;
import com.leveluparcade.entity.EstadoDevolucion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DevolucionService {

    /**
     * Crea una solicitud de devolucion para el pedido indicado.
     * El cliente debe ser dueno del pedido (o el llamante debe ser ADMIN).
     *
     * Reglas:
     * - El pedido debe estar en estado ENTREGADO.
     * - Debe estar dentro de la ventana temporal (14 dias por defecto).
     * - Las cantidades pedidas no pueden superar lo aun devolvible.
     */
    DevolucionResponse crearDevolucion(CrearDevolucionRequest request);

    /**
     * Listado paginado (ADMIN). Filtros opcionales.
     */
    Page<DevolucionResponse> listarDevoluciones(EstadoDevolucion estado,
                                                Long clienteId,
                                                Pageable pageable);

    /**
     * Devoluciones del cliente autenticado.
     */
    Page<DevolucionResponse> listarMisDevoluciones(Pageable pageable);

    /**
     * Detalle de una devolucion concreta. ADMIN o dueno.
     */
    DevolucionResponse obtenerDevolucion(Long id);

    /**
     * Aprueba la devolucion: cambia estado a APROBADA, calcula importe,
     * acumula cantidad_devuelta en lineas_pedido y restaura stock.
     */
    DevolucionResponse aprobarDevolucion(Long id);

    /**
     * Rechaza la devolucion. Requiere motivo de rechazo. Sin restauracion
     * de stock ni cambios en cantidades devueltas.
     */
    DevolucionResponse rechazarDevolucion(Long id, RechazarDevolucionRequest request);

    /**
     * Marca la devolucion como COMPLETADA tras realizar el reembolso real
     * (fuera del sistema en V1).
     */
    DevolucionResponse completarDevolucion(Long id);
}