package com.vcc.template.kong.service;

import java.util.Map;

/**
 * Service for provisioning JWT authentication on Kong Gateway.
 *
 * <p>Handles the lifecycle of Kong Consumers and JWT credentials needed
 * for centralized authentication. Supports dynamic Identity Provider (IdP)
 * configuration per project, allowing each project to use either
 * the system's default Keycloak realm or a custom IdP.</p>
 */
public interface KongJwtProvisioningService {

    /**
     * Provisions JWT authentication for a project by:
     * 1. Fetching the JWKS public keys from the project's configured IdP
     * 2. Creating a Kong Consumer for the project
     * 3. Adding JWT credentials with the fetched public key
     *
     * @param projectCode the project code
     * @return the created Kong Consumer details
     */
    Map<String, Object> provisionJwtForProject(Long kongInstanceId, String projectCode);

    /**
     * Enables JWT plugin on a specific Kong Service or Route.
     *
     * @param projectCode the project code
     * @param entityType  "services" or "routes"
     * @param entityId    the Kong entity ID
     * @return the created plugin details
     */
    Map<String, Object> enableJwtPlugin(Long kongInstanceId, String projectCode, String entityType, String entityId);

    /**
     * Enables ACL plugin on a specific Kong Service or Route to restrict access to a specific group.
     *
     * @param projectCode  the project code
     * @param entityType   "services" or "routes"
     * @param entityId     the Kong entity ID
     * @param allowedGroup the group name allowed to access (e.g., "group-realm-rbac-management")
     * @return the created plugin details
     */
    Map<String, Object> enableAclPlugin(Long kongInstanceId, String projectCode, String entityType, String entityId, String allowedGroup);

    /**
     * Determines the correct ACL group for a project based on its configured Client ID.
     *
     * @param projectCode the project code
     * @return the ACL group name
     */
    String getAclGroupForProject(String projectCode);

    /**
     * Removes JWT provisioning (Consumer + credentials) for a project.
     *
     * @param projectCode the project code
     */
    void deprovisionJwtForProject(Long kongInstanceId, String projectCode);

    /**
     * Synchronizes JWT plugin state across all Kong Services for a project.
     * If enabled is true, adds jwt and acl plugins if they are missing.
     * If enabled is false, removes them.
     */
    void syncJwtAuthForProject(String projectCode, boolean enabled);
}
