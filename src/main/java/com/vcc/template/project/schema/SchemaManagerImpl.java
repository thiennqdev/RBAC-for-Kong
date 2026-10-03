package com.vcc.template.project.schema;

import com.vcc.template.project.errorcode.ProjectErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
class SchemaManagerImpl implements SchemaManager {

    private static final Pattern SCHEMA_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{2,49}$");

    // PostgreSQL reserved words that cannot be used as schema names
    private static final Set<String> RESERVED_WORDS = Set.of(
            "public", "information_schema", "pg_catalog", "pg_toast", "pg_temp",
            "user", "current_user", "session_user", "current_schema", "current_database");

    private final DataSource dataSource;

    @Override
    public void createSchema(String schemaName) {
        validateSchemaName(schemaName);

        // Normalize schema name to lowercase to avoid PostgreSQL identifier casing
        // issues
        String normalizedSchemaName = schemaName.toLowerCase();
        String sql = String.format("CREATE SCHEMA IF NOT EXISTS \"%s\"", normalizedSchemaName);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            log.atDebug()
                    .setMessage("Creating schema: {} (normalized from: {})")
                    .addArgument(normalizedSchemaName)
                    .addArgument(schemaName)
                    .log();

            statement.execute(sql);

            log.atInfo()
                    .setMessage("Successfully created schema: {}")
                    .addArgument(normalizedSchemaName)
                    .log();

        } catch (SQLException e) {
            log.atError()
                    .setMessage("Failed to create schema: {}")
                    .addArgument(normalizedSchemaName)
                    .setCause(e)
                    .log();

            throw ProjectErrorCode.SCHEMA_CREATION_FAILED.toException(schemaName);
        }
    }

    @Override
    public boolean schemaExists(String schemaName) {
        // Normalize schema name to lowercase for consistent checking
        String normalizedSchemaName = schemaName.toLowerCase();
        String sql = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";

        try (Connection connection = dataSource.getConnection();
                var preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, normalizedSchemaName);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
                return false;
            }

        } catch (SQLException e) {
            log.atError()
                    .setMessage("Failed to check schema existence: {}")
                    .addArgument(normalizedSchemaName)
                    .setCause(e)
                    .log();

            return false;
        }
    }

    @Override
    public void validateSchemaName(String schemaName) {
        if (!StringUtils.hasText(schemaName)) {
            throw ProjectErrorCode.INVALID_PROJECT_CODE.toException();
        }

        // Check pattern: starts with a letter, 3-50 chars,
        // alphanumeric/underscore/hyphen
        if (!SCHEMA_NAME_PATTERN.matcher(schemaName).matches()) {
            throw ProjectErrorCode.INVALID_PROJECT_CODE.toException();
        }

        // Check reserved words
        if (RESERVED_WORDS.contains(schemaName.toLowerCase())) {
            throw ProjectErrorCode.INVALID_PROJECT_CODE.toException();
        }
    }

    @Override
    public void dropSchema(String schemaName) {
        String normalizedSchemaName = schemaName.toLowerCase();

        // Extra safety: never allow dropping system schemas
        if (RESERVED_WORDS.contains(normalizedSchemaName) || "public".equals(normalizedSchemaName)) {
            throw ProjectErrorCode.INVALID_PROJECT_CODE.toException();
        }

        String sql = String.format("DROP SCHEMA IF EXISTS \"%s\" CASCADE", normalizedSchemaName);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            log.warn("Dropping schema permanently: {} — this is irreversible!", normalizedSchemaName);
            statement.execute(sql);
            log.info("Schema dropped successfully: {}", normalizedSchemaName);

        } catch (SQLException e) {
            log.error("Failed to drop schema: {}", normalizedSchemaName, e);
            throw ProjectErrorCode.SCHEMA_DROP_FAILED.toException(schemaName);
        }
    }
}
