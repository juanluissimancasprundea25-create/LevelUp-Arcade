package com.leveluparcade.config;

import com.leveluparcade.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracion de seguridad con DOS cadenas independientes:
 *
 *  1) apiSecurityFilterChain  (@Order(1)) -> /api/** -> JWT + STATELESS
 *     Para clientes API (mobile, integraciones, tests JWT).
 *
 *  2) adminSecurityFilterChain (@Order(2)) -> /admin/** -> form-login + sesion + CSRF
 *     Para el panel web AdminLTE (solo ROLE_ADMIN).
 *
 * <p>Las rutas publicas (landing /, recursos estaticos, /facturas/verificar/**,
 * y la futura tienda cliente en raiz) caen fuera de ambas cadenas y son
 * accesibles sin login.
 *
 * <p>IMPORTANTE: el orden importa. Spring evalua las cadenas por @Order ascendente.
 * Si una peticion encaja con el securityMatcher de la primera, las siguientes
 * no se evaluan.
 *
 * <p>Cuando se anada el login de cliente (PR siguiente), se introducira una
 * tercera cadena @Order(3) con securityMatcher para /cuenta/** o similar.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Cadena 1: API REST con JWT (STATELESS).
     * Solo aplica a /api/** y deja /api/auth/** publico para login JWT.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/facturas/verificar/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cadena 2: panel admin con form-login. Solo aplica a /admin/**.
     * Todo lo que esta bajo /admin/** requiere ROLE_ADMIN, excepto la propia
     * pagina de login (/admin/login).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login").permitAll()
                .anyRequest().hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/admin/dashboard", true)
                .failureUrl("/admin/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
            );

        // CSRF queda ACTIVADO por defecto para form-login.
        return http.build();
    }

    /**
     * Cadena 3: rutas publicas.
     *
     * <p>Cubre la landing /, recursos estaticos, /facturas/verificar/** (QR),
     * /error, y CUALQUIER otra ruta no cubierta por las cadenas anteriores.
     * Esto deja preparado el terreno para que la futura tienda cliente
     * (catalogo, registro, etc) viva en la raiz sin necesitar login.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()
            )
            // CSRF desactivado en publico porque aun no hay formularios aqui.
            // Cuando se anada registro de cliente / carrito, se evaluara reactivarlo
            // solo para esas rutas, o moverlas a una cadena propia con CSRF on.
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}