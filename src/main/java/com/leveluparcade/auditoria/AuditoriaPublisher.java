package com.leveluparcade.auditoria;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Fachada simple sobre {@link ApplicationEventPublisher} para publicar
 * eventos de auditoria desde los Services.
 *
 * <p>Existe para no tener que inyectar el publisher de Spring directamente
 * en cada Service: solo {@code AuditoriaPublisher}, que es mas explicito
 * y mas facil de mockear en tests.
 */
@Component
public class AuditoriaPublisher {

    private final ApplicationEventPublisher publisher;

    public AuditoriaPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(AuditoriaEvent event) {
        publisher.publishEvent(event);
    }
}