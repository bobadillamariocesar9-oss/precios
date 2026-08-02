package com.apiprecios.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.apiprecios.repository")
public class JpaConfig {
    // Spring Boot auto-configura DataSource y EntityManagerFactory desde application.yml.
    // Esta clase habilita auditing (@CreatedDate, @LastModifiedDate) y la gestión de
    // transacciones declarativas (@Transactional).
}
