package com.leveluparcade.util;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Utilidades para preparar el SecurityContext en tests unitarios
 * de services que llaman directamente a SecurityContextHolder.
 */
public final class SecurityContextTestUtils {

    private SecurityContextTestUtils() {}

    public static void autenticarComoAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
            "admin@test.local",
            "n/a",
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void autenticarComoCliente() {
        var auth = new UsernamePasswordAuthenticationToken(
            "cliente@test.local",
            "n/a",
            List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    public static void limpiar() {
        SecurityContextHolder.clearContext();
    }
}