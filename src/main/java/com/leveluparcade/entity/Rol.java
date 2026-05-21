package com.leveluparcade.entity;

/**
 * Roles del sistema LevelUp Arcade.
 *
 * <ul>
 *   <li>{@link #ADMIN}: control total sobre todas las funcionalidades del sistema.</li>
 *   <li>{@link #EMPLEADO}: gestión interna (productos, clientes, proveedores) sin
 *       acceso a configuración crítica ni gestión de usuarios.</li>
 *   <li>{@link #CLIENTE}: usuario final que puede realizar pedidos, ver su historial,
 *       solicitar devoluciones y contactar con el administrador.</li>
 * </ul>
 *
 * Se almacena como VARCHAR en la tabla {@code usuarios} (constraint CHECK definido
 * en {@code V1__init.sql}).
 */
public enum Rol {
    ADMIN,
    EMPLEADO,
    CLIENTE
}