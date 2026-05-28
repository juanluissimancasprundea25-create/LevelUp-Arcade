package com.leveluparcade.service;

import com.leveluparcade.entity.Carrito;

/**
 * Logica de negocio del carrito de compra del cliente autenticado.
 *
 * <p>Todas las operaciones actuan sobre el carrito del cliente que esta
 * logueado (resuelto via SecurityHelper). No reciben el cliente por
 * parametro a proposito: el carrito es siempre "el mio".
 *
 * <p>Reglas:
 * <ul>
 *   <li>Si el cliente no tiene carrito, se crea al vuelo.</li>
 *   <li>No se puede anadir mas cantidad que el stock disponible.</li>
 *   <li>Solo se pueden modificar/eliminar lineas del propio carrito.</li>
 * </ul>
 */
public interface CarritoService {

    /** Devuelve (creandolo si hace falta) el carrito del cliente logueado. */
    Carrito obtenerCarritoActual();

    /**
     * Anade una cantidad de un producto al carrito. Si el producto ya
     * estaba, suma la cantidad. Valida stock.
     *
     * @throws IllegalArgumentException si la cantidad es invalida o supera stock
     */
    Carrito anadirProducto(Long productoId, int cantidad);

    /**
     * Fija la cantidad de una linea. Si la cantidad es 0, elimina la linea.
     * Valida stock y propiedad de la linea.
     */
    Carrito cambiarCantidad(Long lineaId, int cantidad);

    /** Elimina una linea del carrito (valida propiedad). */
    Carrito eliminarLinea(Long lineaId);

    /** Vacia por completo el carrito del cliente logueado. */
    void vaciar();

    /**
     * Numero de unidades en el carrito del cliente logueado (para el badge).
     * Devuelve 0 si no hay sesion de cliente o no hay carrito.
     */
    int contarUnidades();
}
