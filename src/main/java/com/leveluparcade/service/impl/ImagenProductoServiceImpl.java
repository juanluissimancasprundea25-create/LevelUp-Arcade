package com.leveluparcade.service.impl;

import com.leveluparcade.config.UploadsProperties;
import com.leveluparcade.service.ImagenProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Implementacion del procesado de imagenes de producto.
 *
 * <p>Algoritmo:
 * <ol>
 *   <li>Validar MIME (image/jpeg, image/png, image/webp). image/webp se
 *       acepta como entrada pero la salida siempre es JPEG, asi que en
 *       la practica solo entran JPEG/PNG si la JVM no tiene plugin WebP.</li>
 *   <li>Leer en BufferedImage. Si {@code ImageIO.read} devuelve null,
 *       el fichero no es una imagen valida -> rechazo.</li>
 *   <li>Recorte centrado a un cuadrado (lado = min(ancho, alto)).</li>
 *   <li>Redimensionado a {@code tamano x tamano} con interpolacion
 *       bilineal sobre canvas RGB (descarta alfa de PNGs transparentes
 *       sobre fondo blanco - mejor que perderlo en JPEG).</li>
 *   <li>Escritura como JPEG con calidad configurable a un fichero
 *       temporal y move atomico al destino final.</li>
 * </ol>
 *
 * <p>El nombre del fichero es un UUID + ".jpg", lo que da:
 * <ul>
 *   <li>Cero colisiones.</li>
 *   <li>Cache HTTP segura: si el producto cambia su imagen, se genera
 *       otro UUID; la antigua se borra del disco.</li>
 *   <li>Imposibilidad de adivinar URLs ajenas.</li>
 * </ul>
 */
@Service
public class ImagenProductoServiceImpl implements ImagenProductoService {

    private static final Logger log = LoggerFactory.getLogger(ImagenProductoServiceImpl.class);

    private static final Set<String> MIMES_ACEPTADOS = Set.of(
            "image/jpeg", "image/jpg", "image/pjpeg",
            "image/png",
            "image/webp"
    );

    private static final String SUBDIR = "productos";

    private final UploadsProperties properties;

    public ImagenProductoServiceImpl(UploadsProperties properties) {
        this.properties = properties;
    }

    @Override
    public String guardar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("La imagen esta vacia.");
        }
        long maxBytes = properties.getImagenProducto().getMaxBytes();
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "La imagen supera el tamano maximo permitido (" +
                    (maxBytes / 1024 / 1024) + " MB).");
        }
        String mime = file.getContentType();
        if (mime == null || !MIMES_ACEPTADOS.contains(mime.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Tipo de imagen no soportado. Usa JPG, PNG o WEBP.");
        }

        BufferedImage original;
        // Leemos en memoria (file.getBytes) en vez de file.getInputStream()
        // porque ImageIO.read del input stream a veces falla en ciertos JPEGs.
        try (InputStream in = new ByteArrayInputStream(file.getBytes())) {
            original = ImageIO.read(in);
        } catch (IOException ex) {
            throw new IllegalArgumentException(
                    "No se ha podido leer la imagen: " + ex.getMessage(), ex);
        }
        if (original == null) {
            throw new IllegalArgumentException(
                    "El fichero no es una imagen valida o el formato no es soportado.");
        }

        int lado = properties.getImagenProducto().getTamano();
        BufferedImage cuadrada = recortarCuadrado(original);
        BufferedImage escalada = escalar(cuadrada, lado);

        // Generamos nombre unico y escribimos
        String nombre = UUID.randomUUID().toString() + ".jpg";
        Path destinoDir = Paths.get(properties.getPath(), SUBDIR).toAbsolutePath().normalize();
        Path destinoFinal = destinoDir.resolve(nombre);
        Path destinoTmp = destinoDir.resolve(nombre + ".tmp");

        try {
            Files.createDirectories(destinoDir);
            escribirJpeg(escalada, destinoTmp, properties.getImagenProducto().getCalidad());
            Files.move(destinoTmp, destinoFinal, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            // Limpieza best-effort
            try { Files.deleteIfExists(destinoTmp); } catch (IOException ignored) {}
            throw new IllegalArgumentException(
                    "Error guardando la imagen en disco: " + ex.getMessage(), ex);
        }

        String relativePath = SUBDIR + "/" + nombre;
        log.info("Imagen de producto guardada: {} ({} bytes en disco)",
                relativePath, sizeOf(destinoFinal));
        return relativePath;
    }

    @Override
    public void borrarSiExiste(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        // Las URLs externas (datos historicos pre-PR #27) no se borran.
        String lower = relativePath.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) return;
        // Sanity check: no permitir path traversal aunque la BD venga corrupta.
        if (relativePath.contains("..")) {
            log.warn("Path sospechoso en borrarSiExiste, ignorado: {}", relativePath);
            return;
        }
        Path base = Paths.get(properties.getPath()).toAbsolutePath().normalize();
        Path objetivo = base.resolve(relativePath).normalize();
        if (!objetivo.startsWith(base)) {
            log.warn("Intento de borrar fuera del directorio de uploads: {}", objetivo);
            return;
        }
        try {
            boolean borrado = Files.deleteIfExists(objetivo);
            if (borrado) log.info("Imagen borrada: {}", relativePath);
        } catch (IOException ex) {
            log.warn("No se ha podido borrar {}: {}", objetivo, ex.getMessage());
        }
    }

    // ---------- helpers internos ----------

    /**
     * Recorta la imagen al cuadrado centrado mas grande que cabe.
     * Si ya es cuadrada, la devuelve tal cual.
     */
    private static BufferedImage recortarCuadrado(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w == h) return src;
        int lado = Math.min(w, h);
        int x = (w - lado) / 2;
        int y = (h - lado) / 2;
        return src.getSubimage(x, y, lado, lado);
    }

    /**
     * Escala a {@code lado x lado} pixeles sobre un canvas RGB blanco.
     * Descartar alfa simplifica la salida JPEG (que no soporta alfa) y
     * evita imagenes negras en transparencias.
     */
    private static BufferedImage escalar(BufferedImage src, int lado) {
        BufferedImage destino = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        try {
            // Fondo blanco para PNGs con transparencia
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, lado, lado);

            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, lado, lado, null);
        } finally {
            g.dispose();
        }
        return destino;
    }

    private static void escribirJpeg(BufferedImage img, Path destino, float calidad) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(Math.max(0.5f, Math.min(1.0f, calidad)));
        try (ImageOutputStream out = ImageIO.createImageOutputStream(destino.toFile())) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(img, null, null), params);
        } finally {
            writer.dispose();
        }
    }

    private static long sizeOf(Path p) {
        try { return Files.size(p); } catch (IOException ex) { return -1; }
    }
}
