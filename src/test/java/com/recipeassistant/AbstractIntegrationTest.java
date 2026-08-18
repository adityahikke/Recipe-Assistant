package com.recipeassistant;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES;
    private static final boolean DOCKER_AVAILABLE;

    static {
        boolean available;
        PostgreSQLContainer<?> container = null;
        try {
            available = DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            available = false;
        }
        DOCKER_AVAILABLE = available;

        if (DOCKER_AVAILABLE) {
            container = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("recipe_assistant")
                .withUsername("recipe_user")
                .withPassword("recipe_password");
            container.start();
        }
        POSTGRES = container;
    }

    @BeforeAll
    static void checkDockerAvailable() {
        assumeTrue(DOCKER_AVAILABLE,
            "Skipping integration test: Docker is not available in this environment");
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }
}
