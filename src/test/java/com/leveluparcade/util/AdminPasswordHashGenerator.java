package com.leveluparcade.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test utilitario para generar y verificar hashes BCrypt.
 *
 * <p>Uso típico:
 * <ol>
 *   <li>Ejecutar con {@code ./mvnw test -Dtest=AdminPasswordHashGenerator}.</li>
 *   <li>Copiar el hash impreso por consola.</li>
 *   <li>Pegarlo en una migración Flyway o en el seed SQL.</li>
 * </ol>
 *
 * <p>El test usa el MISMO {@link BCryptPasswordEncoder} que la aplicación usa
 * para validar passwords (configurado en {@code SecurityConfig}), evitando
 * desajustes de versión o cost factor.
 *
 * <p>Además, valida que un hash recién generado coincide con la password en
 * plano, lo que sirve como test de regresión para que esto no vuelva a fallar.
 */
class AdminPasswordHashGenerator {

    private static final String ADMIN_PASSWORD_PLANO = "admin123";

    @Test
    void generarYVerificarHashAdmin() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        String hash = encoder.encode(ADMIN_PASSWORD_PLANO);

        System.out.println();
        System.out.println("==========================================================");
        System.out.println(" HASH BCrypt para password '" + ADMIN_PASSWORD_PLANO + "':");
        System.out.println(" " + hash);
        System.out.println("==========================================================");
        System.out.println();

        assertTrue(
            encoder.matches(ADMIN_PASSWORD_PLANO, hash),
            "El hash recién generado debería coincidir con la password en plano"
        );
    }
}