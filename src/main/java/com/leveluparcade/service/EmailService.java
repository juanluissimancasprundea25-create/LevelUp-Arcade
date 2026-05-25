package com.leveluparcade.service;

/**
 * Servicio de envio de emails transaccionales.
 *
 * <p>Si las credenciales SMTP no estan configuradas, las implementaciones
 * deben loguear el contenido pero no fallar, para no bloquear el flujo
 * en entornos de desarrollo.
 */
public interface EmailService {

    /**
     * Envia un email de recuperacion de contrasena con el enlace al token.
     *
     * @param destinatario direccion de email del usuario
     * @param nombreUsuario nombre del usuario para personalizar el saludo
     * @param enlaceReset URL completa con el token para resetear la clave
     */
    void enviarEmailRecuperacionPassword(String destinatario,
                                         String nombreUsuario,
                                         String enlaceReset);
}