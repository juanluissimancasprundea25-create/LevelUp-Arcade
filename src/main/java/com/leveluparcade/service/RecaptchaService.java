package com.leveluparcade.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Duration;
import java.util.Map;

/**
 * Validador de reCAPTCHA v3 contra la API de Google.
 *
 * <p>Modo dev: si {@code RECAPTCHA_SECRET} no esta configurado o tiene un
 * valor placeholder, el servicio devuelve true sin llamar a Google. Esto
 * permite probar el registro en local sin necesidad de claves reales.
 *
 * <p>Modo prod: hace POST a Google y exige {@code success=true} y
 * {@code score >= 0.5}. Un score bajo suele indicar bot.
 *
 * <p>API doc: https://developers.google.com/recaptcha/docs/v3
 */
@Service
public class RecaptchaService {

    private static final Logger log = LoggerFactory.getLogger(RecaptchaService.class);
    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final double UMBRAL_SCORE = 0.5;

    private final String secret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecaptchaService(@Value("${recaptcha.secret:}") String secret) {
        this.secret = secret;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * Verifica un token de reCAPTCHA v3.
     *
     * @param token token devuelto por grecaptcha.execute() en el cliente
     * @return true si es valido o si el captcha esta deshabilitado en dev
     */
    public boolean verificar(String token) {
        if (estaDeshabilitado()) {
            log.debug("reCAPTCHA deshabilitado (sin clave configurada). Aceptando.");
            return true;
        }

        if (token == null || token.isBlank()) {
            log.warn("Token de reCAPTCHA vacio recibido.");
            return false;
        }

        try {
            String body = "secret=" + secret + "&response=" + token;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(VERIFY_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            boolean success = json.path("success").asBoolean(false);
            double score = json.path("score").asDouble(0.0);

            if (!success) {
                log.warn("reCAPTCHA rechazado por Google. Codigos: {}",
                         json.path("error-codes"));
                return false;
            }

            if (score < UMBRAL_SCORE) {
                log.warn("reCAPTCHA con score bajo: {} (umbral {}). Posible bot.",
                         score, UMBRAL_SCORE);
                return false;
            }

            log.debug("reCAPTCHA OK con score {}.", score);
            return true;

        } catch (Exception ex) {
            log.error("Error verificando reCAPTCHA: {}", ex.getMessage());
            // Politica conservadora: si Google no responde, bloqueamos.
            return false;
        }
    }

    /** True si no hay clave configurada o la clave es un placeholder. */
    private boolean estaDeshabilitado() {
        return secret == null
            || secret.isBlank()
            || secret.startsWith("clave-")
            || secret.equals("placeholder");
    }
}