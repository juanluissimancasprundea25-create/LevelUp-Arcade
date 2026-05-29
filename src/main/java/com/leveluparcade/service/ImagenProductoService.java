package com.leveluparcade.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Servicio para gestionar las imagenes de los productos:
 *
 * <ul>
 *   <li>Validacion de tipo MIME (solo image/jpeg, image/png, image/webp).</li>
 *   <li>Validacion de tamano maximo.</li>
 *   <li>Recorte centrado a 1:1 + redimensionado a 600x600.</li>
 *   <li>Salida en JPEG (calidad configurable).</li>
 *   <li>Nombre UUID para evitar colisiones y cache stale.</li>
 *   <li>Borrado seguro de la imagen anterior al reemplazar.</li>
 * </ul>
 *
 * <p>Las imagenes se guardan bajo {@code {uploads.path}/productos/}
 * y se referencian en la BD con el path relativo
 * {@code productos/{uuid}.jpg}. Para mostrarlas en plantillas hay que
 * prefijar {@code /img/} (ver {@link com.leveluparcade.config.WebMvcConfig}).
 */
public interface ImagenProductoService {

    /**
     * Procesa la imagen recibida y la guarda en disco.
     *
     * @param file la imagen subida por el usuario.
     * @return path relativo (por ejemplo {@code productos/abc123.jpg})
     *         para guardar en {@code Producto.imagenUrl}.
     * @throws IllegalArgumentException si el fichero esta vacio, supera
     *         el tamano maximo o no es una imagen valida.
     */
    String guardar(MultipartFile file);

    /**
     * Borra del disco una imagen previamente guardada. Tolerante a fallos:
     * solo loguea si el fichero no existe o si no se puede borrar.
     *
     * @param relativePath el path relativo guardado en BD
     *        (por ejemplo {@code productos/abc123.jpg}). Si es null,
     *        vacio o una URL externa (http/https), no hace nada.
     */
    void borrarSiExiste(String relativePath);
}
