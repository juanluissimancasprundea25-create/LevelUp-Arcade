package com.leveluparcade.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Configura cookies de sesion diferenciadas para que las sesiones de
 * admin (/admin/**) y cliente (resto) no se pisen en el mismo navegador.
 *
 * <p>Funcionamiento: filtro que reescribe el nombre de la cookie de sesion
 * en cada peticion:
 * <ul>
 *   <li>Peticiones a /admin/** -> cookie "JSESSIONID_ADMIN"</li>
 *   <li>Resto de peticiones (tienda) -> cookie "JSESSIONID_CLIENTE"</li>
 * </ul>
 *
 * <p>Sin esto, ambas cadenas comparten JSESSIONID y una sesion (admin)
 * sobreescribe la otra (cliente) al loguearse en el mismo navegador.
 */
@Configuration
public class SessionCookieConfig {

    @Bean
    public OncePerRequestFilter sessionCookieRewriter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain)
                    throws ServletException, IOException {

                boolean esAdmin = req.getRequestURI()
                        .startsWith(req.getContextPath() + "/admin");
                final String cookieName = esAdmin ? "JSESSIONID_ADMIN" : "JSESSIONID_CLIENTE";

                // Wrapper de request: si llega la cookie con nuestro nombre custom,
                // la presentamos a Tomcat como si fuera JSESSIONID normal.
                HttpServletRequestWrapper wrappedReq = new HttpServletRequestWrapper(req) {
                    @Override
                    public Cookie[] getCookies() {
                        Cookie[] originales = super.getCookies();
                        if (originales == null) return null;
                        Cookie[] copia = new Cookie[originales.length];
                        for (int i = 0; i < originales.length; i++) {
                            Cookie c = originales[i];
                            if (cookieName.equals(c.getName())) {
                                Cookie nueva = new Cookie("JSESSIONID", c.getValue());
                                nueva.setPath(c.getPath() != null ? c.getPath() : "/");
                                copia[i] = nueva;
                            } else {
                                copia[i] = c;
                            }
                        }
                        return copia;
                    }
                };

                // Wrapper de response: cuando Tomcat escriba JSESSIONID, lo
                // renombramos al nombre custom de esta cadena.
                HttpServletResponseWrapper wrappedRes = new HttpServletResponseWrapper(res) {
                    @Override
                    public void addCookie(Cookie cookie) {
                        if ("JSESSIONID".equals(cookie.getName())) {
                            Cookie renombrada = new Cookie(cookieName, cookie.getValue());
                            renombrada.setPath(cookie.getPath() != null ? cookie.getPath() : "/");
                            renombrada.setHttpOnly(cookie.isHttpOnly());
                            renombrada.setSecure(cookie.getSecure());
                            renombrada.setMaxAge(cookie.getMaxAge());
                            super.addCookie(renombrada);
                        } else {
                            super.addCookie(cookie);
                        }
                    }
                };

                chain.doFilter(wrappedReq, wrappedRes);
            }
        };
    }
}