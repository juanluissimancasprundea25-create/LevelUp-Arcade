package com.leveluparcade.service;

import com.leveluparcade.dto.request.ForgotPasswordRequest;
import com.leveluparcade.dto.request.ResetPasswordRequest;

/**
 * Operaciones del flujo de recuperacion de contrasena.
 */
public interface PasswordResetService {

    /**
     * Inicia el flujo: si el email existe, genera un token y envia el email.
     * Si no existe, no hace nada (no revela la existencia de la cuenta).
     */
    void solicitarRecuperacion(ForgotPasswordRequest request);

    /**
     * Aplica el cambio de contrasena si el token es valido y no esta usado.
     *
     * @throws IllegalArgumentException si el token no existe, esta usado
     *         o ha caducado
     */
    void resetearPassword(ResetPasswordRequest request);
}