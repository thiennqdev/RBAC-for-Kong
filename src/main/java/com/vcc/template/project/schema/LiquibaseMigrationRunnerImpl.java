package com.vcc.template.project.schema;

import com.vcc.template.project.errorcode.ProjectErrorCode;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
class LiquibaseMigrationRunnerImpl implements LiquibaseMigrationRunner {

    private final DataSource dataSource;

    private static final String TENANT_CHANGE_LOG_PATH = "classpath:db/changelog/db.changelog-tenant.yaml";

    @Override
    public void runMigrations(String schemaName) {
        // Normalize schema name to lowercase to match PostgreSQL behavior
        String normalizedSchemaName = schemaName.toLowerCase();

        try (Connection connection = dataSource.getConnection()) {

            log.atDebug()
                    .setMessage("Starting Liquibase tenant migrations for schema: {} (normalized from: {})")
                    .addArgument(normalizedSchemaName)
                    .addArgument(schemaName)
                    .log();

            String originalSchema = connection.getSchema();
            // Set the schema for this connection
            connection.setSchema(normalizedSchemaName);

            // Create Liquibase database instance
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            // Set the default schema for Liquibase operations
            database.setDefaultSchemaName(normalizedSchemaName);
            database.setLiquibaseSchemaName(normalizedSchemaName);

            // Create a Liquibase instance with the tenant-specific changelog
            String changeLogFile = TENANT_CHANGE_LOG_PATH.replace("classpath:", "");
            Liquibase liquibase = new Liquibase(
                    changeLogFile,
                    new ClassLoaderResourceAccessor(),
                    database);

            try {
                // Run all changesets without context or label filters
                liquibase.update(new Contexts(), new LabelExpression());

                log.atInfo()
                        .setMessage("Successfully completed Liquibase tenant migrations for schema: {}")
                        .addArgument(normalizedSchemaName)
                        .log();
            } finally {
                // Restore the original schema BEFORE closing liquibase/connection
                try {
                    connection.setSchema(originalSchema);
                } catch (SQLException ex) {
                    log.atWarn().setMessage("Failed to restore original schema {} on connection")
                            .addArgument(originalSchema).setCause(ex).log();
                }
                liquibase.close();
            }

        } catch (SQLException | LiquibaseException e) {
            log.atError()
                    .setMessage("Failed to run Liquibase migrations for schema: {}")
                    .addArgument(normalizedSchemaName)
                    .setCause(e)
                    .log();

            throw ProjectErrorCode.SCHEMA_CREATION_FAILED.toException(schemaName);
        }
    }
}
