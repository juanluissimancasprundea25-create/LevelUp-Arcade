package com.leveluparcade.dto.response;

/**
 * Respuesta especifica de la operacion "crear cliente".
 *
 * <p>Devuelve el cliente recien creado mas la password temporal
 * generada en plano. Esta password se muestra UNA sola vez al admin
 * que ha creado al cliente y no se almacena en ningun otro sitio:
 * la BD solo guarda el hash BCrypt.
 *
 * <p>Por seguridad, el admin debe comunicarla al cliente por un canal
 * seguro y el cliente deberia cambiarla en su primer login.
 */
public record ClienteCreadoResponse(
    ClienteResponse cliente,
    String passwordTemporal
) {}