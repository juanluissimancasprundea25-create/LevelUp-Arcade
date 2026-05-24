package com.leveluparcade.controller.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Config minima para slices @WebMvcTest:
 *  - Activa @PreAuthorize via @EnableMethodSecurity.
 *  - Define un SecurityFilterChain que permite todo a nivel HTTP,
 *    delegando la autorizacion en los @PreAuthorize de los controllers.
 *  - CSRF deshabilitado para poder hacer POST/PATCH desde MockMvc sin token.
 */
@TestConfiguration
@EnableMethodSecurity
public class WebMvcSecurityTestConfig {

    @Bean
    SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}