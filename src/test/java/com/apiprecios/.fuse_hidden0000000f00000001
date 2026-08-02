package com.apiprecios;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base para tests de integración con MockMvc.
 * Usa PostgreSQL local (apiPrecios_test) configurado en src/test/resources/application.yml.
 * Prerequisito: CREATE DATABASE "apiPrecios_test"; en PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
}
