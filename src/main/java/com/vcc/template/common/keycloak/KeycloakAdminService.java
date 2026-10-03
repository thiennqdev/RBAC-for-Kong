package com.vcc.template.common.keycloak;

import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

/**
 * Service interface for Keycloak Admin operations.
 * Manages user lifecycle in the target Keycloak realm.
 */
public interface KeycloakAdminService {

    /**
     * Create a user in Keycloak and return the assigned Keycloak ID.
     */
    String createUser(String username, String email, String firstName, String lastName, String password);

    /**
     * Update user details in Keycloak.
     */
    void updateUser(String keycloakId, String email, String firstName, String lastName);

    /**
     * Enable or disable a user in Keycloak.
     */
    void setUserEnabled(String keycloakId, boolean enabled);

    /**
     * Delete a user from Keycloak (used for compensating transaction rollback).
     */
    void deleteUser(String keycloakId);

    /**
     * Get a user representation by Keycloak ID.
     */
    UserRepresentation getUserById(String keycloakId);

    /**
     * List all users in the target realm.
     */
    List<UserRepresentation> listUsers();

    /**
     * List users that have a specific realm role.
     */
    List<UserRepresentation> getUsersByRealmRole(String roleName);

    /**
     * Create an OpenID Connect Client in Keycloak and return the generated Client Secret.
     * Enable Service Accounts (Client Credentials grant) by default.
     */
    String createClient(String clientId);
}
