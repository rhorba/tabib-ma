package com.tabibma.testsupport;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that need a real Postgres — required rather than an in-memory
 * substitute per docs/test-strategy-tabib-ma.md Section 5 (real DB behavior, e.g. the booking
 * module's EXCLUDE constraint in later epics, is untestable with mocks/H2).
 *
 * POSTGRES is a singleton container shared by every subclass (static field on this base class).
 * It is deliberately NOT annotated with JUnit's {@code @Testcontainers}/{@code @Container} — that
 * combination starts/stops the container around each *test class*, and since every subclass shares
 * the same static instance, the second test class to run would get a container already stopped by
 * the first. Starting it once here keeps it alive for the whole JVM; Testcontainers' Ryuk reaper
 * cleans it up when the session ends.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestJwtKeyConfig.class)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tabibma_test")
            .withUsername("tabibma")
            .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
