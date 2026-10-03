package com.vcc.template.project.schema;

/**
 * Interface for running Liquibase migrations on specific schemas.
 * Used during project creation to initialize tenant-specific database structures.
 */
public interface LiquibaseMigrationRunner {
    
    /**
     * Executes all Liquibase changesets from the master changelog
     * targeting the specified schema.
     *
     * @param schemaName the schema to run migrations against
     * @throws com.vcc.template.common.exception.BusinessException if migration fails
     */
    void runMigrations(String schemaName);
}
