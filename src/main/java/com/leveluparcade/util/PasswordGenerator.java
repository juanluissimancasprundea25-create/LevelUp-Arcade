package com.leveluparcade.util;

import java.security.SecureRandom;

/**
 * Generador de passwords temporales seguros.
 *
 * <p>Usado al dar de alta clientes desde el panel admin: se genera una
 * password aleatoria, se hashea con BCrypt en el Service, y se muestra
 * UNA sola vez al admin para que se la comunique al cliente. Si el
 * cliente la pierde, debera usar el flujo de recuperacion de password.
 *
 * <p>Caracteristicas:
 * <ul>
 *   <li>Longitud configurable (default 12)</li>
 *   <li>Usa SecureRandom (no Random)</li>
 *   <li>Alfabeto sin caracteres ambiguos (no 0/O ni 1/l/I)</li>
 *   <li>Garantiza al menos 1 mayuscula, 1 minuscula, 1 digito y 1 simbolo</li>
 * </ul>
 */
public final class PasswordGenerator {

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITOS    = "23456789";
    private static final String SIMBOLOS   = "!@#$%&*+-=?";
    private static final String TODO       = MAYUSCULAS + MINUSCULAS + DIGITOS + SIMBOLOS;

    private static final int LONGITUD_POR_DEFECTO = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
        // utility class
    }

    /** Genera una password temporal de longitud por defecto (12 chars). */
    public static String generar() {
        return generar(LONGITUD_POR_DEFECTO);
    }

    /**
     * Genera una password temporal de la longitud indicada (minimo 8).
     *
     * @throws IllegalArgumentException si {@code longitud < 8}
     */
    public static String generar(int longitud) {
        if (longitud < 8) {
            throw new IllegalArgumentException(
                "La longitud minima de una password temporal es 8");
        }

        StringBuilder sb = new StringBuilder(longitud);

        // garantizar al menos uno de cada tipo
        sb.append(charAleatorio(MAYUSCULAS));
        sb.append(charAleatorio(MINUSCULAS));
        sb.append(charAleatorio(DIGITOS));
        sb.append(charAleatorio(SIMBOLOS));

        // rellenar el resto con cualquier caracter
        for (int i = 4; i < longitud; i++) {
            sb.append(charAleatorio(TODO));
        }

        // shuffle Fisher-Yates para que los 4 garantizados no esten siempre al inicio
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }

        return new String(arr);
    }

    private static char charAleatorio(String pool) {
        return pool.charAt(RANDOM.nextInt(pool.length()));
    }
}