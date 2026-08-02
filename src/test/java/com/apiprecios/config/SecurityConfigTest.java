package com.apiprecios.config;

import com.apiprecios.AbstractIntegrationTest;
import com.apiprecios.entity.User;
import com.apiprecios.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SecurityConfig")
class SecurityConfigTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.save(User.builder()
            .username("testuser")
            .email("test@test.com")
            .password(passwordEncoder.encode("secret123"))
            .build());
    }

    // ─── Endpoints públicos ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/users es accesible sin autenticación")
    void getUsers_isPublic() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/users (registro) es accesible sin autenticación")
    void registerUser_isPublic() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "nuevo",
                      "email": "nuevo@test.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Swagger UI es accesible sin autenticación")
    void swaggerUi_isPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("OpenAPI docs son accesibles sin autenticación")
    void openApiDocs_isPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk());
    }

    // ─── Endpoints protegidos ─────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/users/{id} sin auth devuelve 401")
    void updateUser_withoutAuth_returns401() throws Exception {
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"x","email":"x@x.com","password":"p"}
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/users/{id} sin auth devuelve 401")
    void deleteUser_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isUnauthorized());
    }

    // ─── Autenticación Basic ──────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/users/{id} con credenciales válidas devuelve 204 o 404")
    void deleteUser_withValidAuth_isProcessed() throws Exception {
        Integer id = userRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(delete("/api/users/" + id)
                .with(httpBasic("testuser", "secret123")))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PUT /api/users/{id} con credenciales incorrectas devuelve 401")
    void updateUser_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(put("/api/users/1")
                .with(httpBasic("testuser", "wrongpassword"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"x","email":"x@x.com","password":"p"}
                    """))
            .andExpect(status().isUnauthorized());
    }
}
