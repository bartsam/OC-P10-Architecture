package com.yourcaryourway.poc;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base PostgreSQL temporaire pour les tests d'intégration
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  /**
   * Conteneur PostgreSQL
   * {@code @ServiceConnection} configure la connexion JDBC et Flyway.
   *
   * @return le conteneur géré par Spring
   */
  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"));
  }
}