package com.apiprecios.config;

import com.apiprecios.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para la configuración CORS en WebConfig.
 * Envía peticiones OPTIONS (preflight) y verifica las cabeceras de respuesta.
 */
@DisplayName("WebConfig - CORS")
class WebConfigTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "http://evil.com";

    // ─── Preflight desde origen permitido ────────────────────────────────────

    @Test
    @DisplayName("OPTIONS /api/** desde origen permitido → 200 con cabeceras CORS")
    void preflight_fromAllowedOrigin_returnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/users")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().exists(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
    }

    @Test
    @DisplayName("OPTIONS /api/** → permite métodos GET, POST, PUT, DELETE, PATCH")
    void preflight_allowsExpectedMethods() throws Exception {
        mockMvc.perform(options("/api/users")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("OPTIONS /api/** → Access-Control-Allow-Credentials: true")
    void preflight_allowsCredentials() throws Exception {
        mockMvc.perform(options("/api/users")
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("OPTIONS /api/** desde origen http://localhost:4200 → permitido")
    void preflight_fromAngularOrigin_isAllowed() throws Exception {
        mockMvc.perform(options("/api/users")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    @DisplayName("OPTIONS /api/** desde origen http://localhost:5173 → permitido (Vite)")
    void preflight_fromViteOrigin_isAllowed() throws Exception {
        mockMvc.perform(options("/api/users")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    // ─── Preflight desde origen NO permitido ─────────────────────────────────

    @Test
    @DisplayName("OPTIONS /api/** desde origen no permitido → sin cabecera Allow-Origin")
    void preflight_fromDisallowedOrigin_noCorsHeader() throws Exception {
        mockMvc.perform(options("/api/users")
                        .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
