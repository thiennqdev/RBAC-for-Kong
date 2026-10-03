package com.vcc.template.project.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class LiquibaseMigrationRunnerTest {

    @ServiceConnection
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SchemaManager schemaManager;

    @Autowired
    private LiquibaseMigrationRunner liquibaseMigrationRunner;

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldRunMigrationsWithUppercaseSchemaName() throws Exception {
        // Given
        String uppercaseSchemaName = "TESTSCHEMA";
        
        // Create schema first
        schemaManager.createSchema(uppercaseSchemaName);
        
        // When
        liquibaseMigrationRunner.runMigrations(uppercaseSchemaName);
        
        // Then - verify tenant schema exists but does NOT contain projects table
        // (projects table should only exist in public schema)
        String normalizedSchemaName = uppercaseSchemaName.toLowerCase();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Verify schema exists
            String schemaExistsQuery = String.format(
                "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = '%s')", 
                normalizedSchemaName
            );
            
            try (ResultSet resultSet = statement.executeQuery(schemaExistsQuery)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean(1)).isTrue();
            }
            
            // Verify projects table does NOT exist in tenant schema (it should only be in public)
            String projectsTableQuery = String.format(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = '%s' AND table_name = 'projects')", 
                normalizedSchemaName
            );
            
            try (ResultSet resultSet = statement.executeQuery(projectsTableQuery)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean(1)).isFalse(); // Should be false - no projects table in tenant schema
            }
            
            // Verify projects table exists in public schema
            String publicProjectsQuery = 
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_name = 'projects')";
            
            try (ResultSet resultSet = statement.executeQuery(publicProjectsQuery)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean(1)).isTrue(); // Should be true - projects table in public schema
            }
        }
    }

    @Test
    void shouldRunMigrationsWithLowercaseSchemaName() throws Exception {
        // Given
        String lowercaseSchemaName = "testschema2";
        
        // Create schema first
        schemaManager.createSchema(lowercaseSchemaName);
        
        // When
        liquibaseMigrationRunner.runMigrations(lowercaseSchemaName);
        
        // Then - verify tenant schema exists but does NOT contain projects table
        // (projects table should only exist in public schema)
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Verify schema exists
            String schemaExistsQuery = String.format(
                "SELECT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = '%s')", 
                lowercaseSchemaName
            );
            
            try (ResultSet resultSet = statement.executeQuery(schemaExistsQuery)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean(1)).isTrue();
            }
            
            // Verify projects table does NOT exist in tenant schema (it should only be in public)
            String projectsTableQuery = String.format(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = '%s' AND table_name = 'projects')", 
                lowercaseSchemaName
            );
            
            try (ResultSet resultSet = statement.executeQuery(projectsTableQuery)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean(1)).isFalse(); // Should be false - no projects table in tenant schema
            }
            
            // Verify projects table exists in public schema
            String publicProjectsQuery = 
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                "WHERE table_schema = 'public' AND table_name = 'projects')";
            
            try (ResultSet resultSet = statement.executeQuery(publicProjectsQuery)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getBoolean(1)).isTrue(); // Should be true - projects table in public schema
            }
        }
    }
}