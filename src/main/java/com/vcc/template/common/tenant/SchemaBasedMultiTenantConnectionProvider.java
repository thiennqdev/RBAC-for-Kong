package com.vcc.template.common.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides database connections with tenant-specific schema isolation.
 * Implements Hibernate's MultiTenantConnectionProvider for schema-based
 * multi-tenancy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaBasedMultiTenantConnectionProvider
        extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> {
    private final DataSource dataSource;

    @Override
    protected DataSource selectAnyDataSource() {
        return dataSource;
    }

    @Override
    protected DataSource selectDataSource(String s) {
        return dataSource;
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        String tenantId = tenantIdentifier != null ? tenantIdentifier : "public";
        log.debug("Acquiring connection for tenant {}", tenantId);
        Connection connection = getAnyConnection();
        try (java.sql.Statement stmt = connection.createStatement()) {
            // Use explicit SQL to append public schema, because connection.setSchema()
            // escapes strings and breaks multi-schema search paths in PGJDBC
            stmt.execute("SET search_path TO \"" + tenantIdentifier + "\", public");
            log.debug("Set search_path to: {}, public", tenantIdentifier);
        } catch (SQLException e) {
            log.error("Failed to set schema to: {}", tenantIdentifier, e);
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) {
        try {
            connection.setSchema("public");
            releaseAnyConnection(connection);
        } catch (SQLException e) {
            log.error("Failed to reset schema", e);
        }
    }
}
