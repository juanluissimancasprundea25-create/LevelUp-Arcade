package com.leveluparcade.auditoria;

import com.leveluparcade.entity.AuditoriaLog;
import com.leveluparcade.repository.AuditoriaLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Escucha eventos de auditoria y los persiste en la BD.
 *
 * <p>Es el unico componente de la aplicacion que escribe en la tabla
 * {@code auditoria_log}. El resto del codigo solo publica eventos.
 *
 * <p>Captura la IP de origen automaticamente del request HTTP actual,
 * si lo hay (no hay request en flujos async o programados, en cuyo
 * caso queda null).
 */
@Component
public class AuditoriaListener {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaListener.class);

    private final AuditoriaLogRepository repository;

    public AuditoriaListener(AuditoriaLogRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void onAuditoriaEvent(AuditoriaEvent event) {
        try {
            AuditoriaLog entry = AuditoriaLog.builder()
                    .usuarioId(event.usuarioId())
                    .accion(event.tipo().name())
                    .entidad(event.entidad())
                    .entidadId(event.entidadId())
                    .descripcion(event.descripcion())
                    .ipOrigen(extraerIpOrigen())
                    .build();
            repository.save(entry);
        } catch (Exception ex) {
            // Nunca dejar que un fallo de auditoria rompa el flujo principal
            log.error("Error al persistir entrada de auditoria: {}", ex.getMessage(), ex);
        }
    }

    private String extraerIpOrigen() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest req = attrs.getRequest();
            // Si hay un proxy/reverse-proxy delante, X-Forwarded-For lleva la IP real
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception ex) {
            return null;
        }
    }
}