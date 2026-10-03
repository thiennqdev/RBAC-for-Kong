package com.vcc.template.project.schema;

/**
 * Interface for managing PostgreSQL schemas for multi-tenant projects.
 * Each project gets its own isolated schema for data separation.
 */
public interface SchemaManager {

    /**
     * Creates a new PostgreSQL schema with the given name.
     * Uses CREATE SCHEMA IF NOT EXISTS to avoid errors if schema already exists.
     *
     * @param schemaName the name of the schema to create
     * @throws com.vcc.template.common.exception.BusinessException if schema
     *                                                             creation fails
     */
    void createSchema(String schemaName);

    /**
     * Checks if a schema exists in the database.
     *
     * @param schemaName the name of the schema to check
     * @return true if the schema exists, false otherwise
     */
    boolean schemaExists(String schemaName);

    /**
     * Validates that a schema name meets PostgreSQL naming requirements.
     * Rules:
     * - Contains only alphanumeric characters, underscores, and hyphens
     * - Starts with a letter
     * - Length between 3-50 characters
     * - Not a PostgreSQL reserved word
     *
     * @param schemaName the schema name to validate
     * @throws com.vcc.template.common.exception.BusinessException if validation
     *                                                             fails
     */
    void validateSchemaName(String schemaName);

    /**
     * Drops (permanently deletes) a PostgreSQL schema and all its contents.
     * Uses DROP SCHEMA CASCADE to remove the schema and all contained objects.
     * ⚠️ This is irreversible — all data in the schema will be permanently lost.
     *
     * @param schemaName the name of the schema to drop
     * @throws com.vcc.template.common.exception.BusinessException if the drop fails
     */
    void dropSchema(String schemaName);
}
