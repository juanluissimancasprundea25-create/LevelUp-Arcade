package com.leveluparcade.config;

import com.leveluparcade.security.JwtFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /** Cadena 1: API REST con JWT. */
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
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** Cadena 2: solo para PROTEGER /admin/** (ya logueado).
     *  No tiene formulario propio. El login se hace en /login (cadena 3). */
    @Bean
    @Order(2)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("ADMIN")
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID_ADMIN", "JSESSIONID_CLIENTE")
                .permitAll()
            )
            // Si un usuario no admin entra a /admin/**, lo mandamos al login publico
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((req, res, e) ->
                        res.sendRedirect(req.getContextPath() + "/login?error"))
            );
        return http.build();
    }

    /**
     * Cadena 3: LOGIN UNICO PUBLICO.
     *
     * <p>Un solo formulario /login. Spring detecta el rol y redirige:
     * <ul>
     *   <li>ROLE_ADMIN -> /admin/dashboard</li>
     *   <li>ROLE_CLIENTE -> /cuenta</li>
     *   <li>otro -> /</li>
     * </ul>
     *
     * <p>Esta cadena tambien protege /cuenta/** (solo CLIENTE).
     */
    @Bean
    @Order(3)
    public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/login", "/login/**", "/registro", "/registro/**", "/logout", "/cuenta/**", "/carrito/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/login/**", "/registro", "/registro/**").permitAll()
                .requestMatchers("/cuenta/**").hasRole("CLIENTE")
                .requestMatchers("/carrito/**").hasRole("CLIENTE")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler((req, res, auth) -> {
                    String rol = auth.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .findFirst()
                            .orElse("");
                    log.info(">>> LOGIN OK: user={} rol={}", auth.getName(), rol);
                    String destino;
                    if ("ROLE_ADMIN".equals(rol)) {
                        destino = "/admin/dashboard";
                    } else if ("ROLE_CLIENTE".equals(rol)) {
                        destino = "/cuenta";
                    } else {
                        destino = "/";
                    }
                    res.sendRedirect(req.getContextPath() + destino);
                })
                .failureHandler((req, res, ex) -> {
                    log.warn(">>> LOGIN FALLO: {}", ex.getMessage());
                    res.sendRedirect(req.getContextPath() + "/login?error");
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID_ADMIN", "JSESSIONID_CLIENTE")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((req, res, e) ->
                        res.sendRedirect(req.getContextPath() + "/login"))
            );
        return http.build();
    }

    /** Cadena 4: publico. */
    @Bean
    @Order(4)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}