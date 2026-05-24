package com.leveluparcade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuracion del modulo de devoluciones.
 * Lee del application.yml bajo el prefijo leveluparcade.devoluciones.
 *
 * Ejemplo:
 *   leveluparcade:
 *     devoluciones:
 *       dias-limite: 14
 */
@Configuration
@ConfigurationProperties(prefix = "leveluparcade.devoluciones")
public class DevolucionesProperties {

    /**
     * Numero maximo de dias tras la entrega del pedido en los que
     * el cliente puede solicitar una devolucion.
     */
    private int diasLimite = 14;

    public int getDiasLimite() {
        return diasLimite;
    }

    public void setDiasLimite(int diasLimite) {
        this.diasLimite = diasLimite;
    }
}