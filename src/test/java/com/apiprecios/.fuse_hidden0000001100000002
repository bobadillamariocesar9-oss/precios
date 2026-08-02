package com.apiprecios;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base para todos los tests de repositorio.
 *
 * @DataJpaTest  → carga solo el slice JPA (sin web, sin servicios).
 * Replace.NONE  → usa el datasource de src/test/resources/application.yml
 *                 (PostgreSQL local, base apiPrecios_test).
 *
 * Prerequisito: CREATE DATABASE "apiPrecios_test";
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public abstract class AbstractRepositoryTest {
}
