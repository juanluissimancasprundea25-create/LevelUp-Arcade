package com.leveluparcade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuracion para la subida de ficheros.
 *
 * <p>Lee del application.yml bajo el prefijo {@code leveluparcade.uploads}:
 * <pre>
 *   leveluparcade:
 *     uploads:
 *       path: ./uploads
 *       imagen-producto:
 *         tamano: 600
 *         calidad: 0.85
 *         max-bytes: 5242880
 * </pre>
 *
 * <p>El {@code path} es el directorio raiz donde se guardan los ficheros
 * subidos. En desarrollo {@code ./uploads} (relativo al directorio de
 * trabajo). En Docker se monta como volumen, por defecto {@code /app/uploads}.
 *
 * <p>Las imagenes de productos se guardan bajo {@code {path}/productos/}
 * y se sirven via {@code /img/productos/} (ver {@link WebMvcConfig}).
 */
@Configuration
@ConfigurationProperties(prefix = "leveluparcade.uploads")
public class UploadsProperties {

    /** Directorio raiz donde se almacenan los ficheros subidos. */
    private String path = "./uploads";

    private final ImagenProducto imagenProducto = new ImagenProducto();

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public ImagenProducto getImagenProducto() { return imagenProducto; }

    /**
     * Parametros del procesado de imagenes de producto.
     * Cuadrado 1:1 para no generar saltos visuales en el catalogo.
     */
    public static class ImagenProducto {

        /** Lado en pixeles de la imagen final (cuadrada). */
        private int tamano = 600;

        /** Calidad JPEG de salida [0.0 .. 1.0]. */
        private float calidad = 0.85f;

        /** Tamano maximo del fichero de entrada en bytes. */
        private long maxBytes = 5L * 1024L * 1024L; // 5 MB

        public int getTamano() { return tamano; }
        public void setTamano(int tamano) { this.tamano = tamano; }

        public float getCalidad() { return calidad; }
        public void setCalidad(float calidad) { this.calidad = calidad; }

        public long getMaxBytes() { return maxBytes; }
        public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }
    }
}
