package com.leveluparcade.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuracion MVC adicional:
 *
 * <p>Mapea {@code /img/**} a la carpeta de uploads en disco para servir
 * las imagenes de producto subidas. Asi una imagen guardada con path
 * {@code productos/abc123.jpg} en la BD se ve en la URL
 * {@code /img/productos/abc123.jpg}.
 *
 * <p>Cache HTTP de 30 dias: los nombres de fichero son UUID, por lo que
 * son inmutables; al cambiar la imagen de un producto se genera un UUID
 * nuevo (no se sobrescribe), evitando problemas de cache stale.
 *
 * <p>Registra {@link UploadsProperties} como bean asociado, asegurando
 * que en cualquier slice de test (@WebMvcTest) que cargue WebMvcConfig
 * (porque es un {@link WebMvcConfigurer}) tambien quede disponible la
 * propiedad.
 */
@Configuration
@EnableConfigurationProperties(UploadsProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);
    private final UploadsProperties properties;

    public WebMvcConfig(UploadsProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploads = Paths.get(properties.getPath()).toAbsolutePath().normalize();
        File dir = uploads.toFile();
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("Creado directorio de uploads: {}", uploads);
            } else {
                log.warn("No se ha podido crear el directorio de uploads: {}", uploads);
            }
        }
        String location = "file:" + uploads.toString().replace('\\', '/') + "/";
        log.info("Sirviendo /img/** desde {}", location);

        registry.addResourceHandler("/img/**")
                .addResourceLocations(location)
                .setCachePeriod(60 * 60 * 24 * 30); // 30 dias
    }
}
