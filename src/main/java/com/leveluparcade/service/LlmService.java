package com.leveluparcade.service;

/**
 * Operaciones de asistencia con LLM para el panel admin.
 *
 * <p>Cada metodo encapsula un prompt especifico y devuelve solo el
 * resultado limpio listo para mostrar al usuario.
 */
public interface LlmService {

    /**
     * Genera una descripcion comercial para un producto.
     *
     * @param nombreProducto nombre del producto
     * @param categoria      nombre de la categoria (puede ser null)
     * @return descripcion de 2-4 frases en castellano
     */
    String generarDescripcionProducto(String nombreProducto, String categoria);

    /**
     * Sugiere una categoria para un producto dado un listado existente.
     *
     * @param nombreProducto      nombre del producto
     * @param descripcion         descripcion (opcional, puede ser null)
     * @param categoriasExistentes nombres de categorias entre las que elegir
     * @return nombre de la categoria mas apropiada (de la lista) o
     *         sugerencia nueva si ninguna encaja
     */
    String sugerirCategoria(String nombreProducto, String descripcion,
                            java.util.List<String> categoriasExistentes);
}