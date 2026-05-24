package com.leveluparcade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuracion del modulo de facturas.
 * Lee del application.yml bajo el prefijo leveluparcade.facturas.
 *
 * Ejemplo:
 *   leveluparcade:
 *     facturas:
 *       prefijo-numero: "FAC"
 *       url-verificacion-base: "http://localhost:8080/api/facturas/verificar"
 *       emisor:
 *         nombre: "LevelUp Arcade S.L."
 *         cif: "B12345678"
 *         direccion: "Calle Falsa 123, 28001 Madrid, Espana"
 *         email: "facturacion@leveluparcade.local"
 */
@Configuration
@ConfigurationProperties(prefix = "leveluparcade.facturas")
public class FacturasProperties {

    /**
     * Prefijo usado en el numero de factura.
     * Formato final: {prefijo}-{anio}-{secuencial}, ej. FAC-2026-00001
     */
    private String prefijoNumero = "FAC";

    /**
     * URL base que se incrustara en el QR de cada factura.
     * El QR apuntara a {urlVerificacionBase}/{numeroFactura}.
     */
    private String urlVerificacionBase = "http://localhost:8080/api/facturas/verificar";

    /**
     * Datos del emisor (la empresa) que apareceran en cabecera del PDF.
     */
    private Emisor emisor = new Emisor();

    public String getPrefijoNumero() {
        return prefijoNumero;
    }

    public void setPrefijoNumero(String prefijoNumero) {
        this.prefijoNumero = prefijoNumero;
    }

    public String getUrlVerificacionBase() {
        return urlVerificacionBase;
    }

    public void setUrlVerificacionBase(String urlVerificacionBase) {
        this.urlVerificacionBase = urlVerificacionBase;
    }

    public Emisor getEmisor() {
        return emisor;
    }

    public void setEmisor(Emisor emisor) {
        this.emisor = emisor;
    }

    /**
     * Bloque anidado con los datos fiscales y de contacto del emisor.
     * Se mapea bajo leveluparcade.facturas.emisor.
     */
    public static class Emisor {

        private String nombre;
        private String cif;
        private String direccion;
        private String email;

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public String getCif() {
            return cif;
        }

        public void setCif(String cif) {
            this.cif = cif;
        }

        public String getDireccion() {
            return direccion;
        }

        public void setDireccion(String direccion) {
            this.direccion = direccion;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}