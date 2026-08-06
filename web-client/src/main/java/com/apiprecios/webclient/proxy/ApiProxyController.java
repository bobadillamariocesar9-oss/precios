package com.apiprecios.webclient.proxy;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Proxy transparente: reenvía todas las llamadas /api/** al backend
 * en el puerto 8081, incluyendo headers de Authorization para HTTP Basic.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiProxyController {

    private final RestClient restClient;

    @Value("${api.backend.url}")
    private String backendUrl;

    // ── GET ────────────────────────────────────────────────────────────────────

    @GetMapping("/**")
    public ResponseEntity<String> proxyGet(
            HttpServletRequest request,
            @RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.GET, extractPath(request), null, headers);
    }

    // ── POST ───────────────────────────────────────────────────────────────────

    @PostMapping("/**")
    public ResponseEntity<String> proxyPost(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.POST, extractPath(request), body, headers);
    }

    // ── PUT ────────────────────────────────────────────────────────────────────

    @PutMapping("/**")
    public ResponseEntity<String> proxyPut(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.PUT, extractPath(request), body, headers);
    }

    // ── DELETE ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/**")
    public ResponseEntity<String> proxyDelete(
            HttpServletRequest request,
            @RequestHeader HttpHeaders headers) {
        return forward(HttpMethod.DELETE, extractPath(request), null, headers);
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private String extractPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        return query != null ? path + "?" + query : path;
    }

    private ResponseEntity<String> forward(
            HttpMethod method, String path, String body, HttpHeaders incomingHeaders) {
        URI uri = URI.create(backendUrl + path);
        try {
            // Construimos un URI absoluto "as-is": evita que RestClient vuelva
            // a codificar caracteres que el query string ya trae codificados
            // (ej. %20 → %2520), lo que rompía las búsquedas con espacios/acentos.
            var spec = restClient.method(method).uri(uri);

            // Pasar Authorization (HTTP Basic) si viene en el request
            String auth = incomingHeaders.getFirst(HttpHeaders.AUTHORIZATION);
            if (auth != null) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, auth);
            }

            ResponseEntity<String> response;
            if (body != null && !body.isBlank()) {
                response = spec.header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(body).retrieve().toEntity(String.class);
            } else {
                response = spec.retrieve().toEntity(String.class);
            }

            // Forzamos UTF-8 explícito en la respuesta — sin esto, caracteres
            // como tildes o guiones largos ("—") pueden llegar corruptos al navegador.
            return ResponseEntity.status(response.getStatusCode())
                    .contentType(MediaType.valueOf("application/json;charset=UTF-8"))
                    .body(response.getBody());

        } catch (HttpStatusCodeException ex) {
            log.warn("Backend respondió {} para {} {} — body: {}",
                    ex.getStatusCode(), method, uri, ex.getResponseBodyAsString(StandardCharsets.UTF_8));
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(MediaType.valueOf("application/json;charset=UTF-8"))
                    .body(ex.getResponseBodyAsString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            log.error("Error en proxy al reenviar {} {}", method, uri, ex);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.valueOf("application/json;charset=UTF-8"))
                    .body("{\"error\":\"" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\"}");
        }
    }
}
