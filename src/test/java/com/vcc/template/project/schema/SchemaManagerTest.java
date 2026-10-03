package com.vcc.template.project.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SchemaManagerTest {

    @ServiceConnection
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SchemaManager schemaManager;

    @Test
    void shouldCreateSchemaWithUppercaseCode() {
        // Given
        String uppercaseCode = "IOC";
        
        // When
        schemaManager.createSchema(uppercaseCode);
        
        // Then - schema should exist (normalized to lowercase)
        assertThat(schemaManager.schemaExists(uppercaseCode)).isTrue();
    }

    @Test
    void shouldCreateSchemaWithLowercaseCode() {
        // Given
        String lowercaseCode = "test";
        
        // When
        schemaManager.createSchema(lowercaseCode);
        
        // Then
        assertThat(schemaManager.schemaExists(lowercaseCode)).isTrue();
    }

    @Test
    void shouldCreateSchemaWithMixedCaseCode() {
        // Given
        String mixedCaseCode = "MixedCase";
        
        // When
        schemaManager.createSchema(mixedCaseCode);
        
        // Then
        assertThat(schemaManager.schemaExists(mixedCaseCode)).isTrue();
    }
}