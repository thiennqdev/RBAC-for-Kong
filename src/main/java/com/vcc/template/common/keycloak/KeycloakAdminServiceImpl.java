package com.vcc.template.common.keycloak;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of KeycloakAdminService using keycloak-admin-client.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private final Keycloak keycloakAdmin;

    @Value("${keycloak.auth.realm}")
    private String targetRealm;

    private RealmResource realmResource() {
        return keycloakAdmin.realm(targetRealm);
    }

    private UsersResource usersResource() {
        return realmResource().users();
    }

    @Override
    public String createUser(String username, String email, String firstName, String lastName, String password) {
        log.debug("Creating Keycloak user: {}", username);

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(username);
        userRep.setEmail(email);
        userRep.setFirstName(firstName);
        userRep.setLastName(lastName);
        userRep.setEnabled(true);
        userRep.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(true);
        userRep.setCredentials(Collections.singletonList(credential));

        // Force password change on first login
        userRep.setRequiredActions(List.of("UPDATE_PASSWORD"));

        try (Response response = usersResource().create(userRep)) {
            if (response.getStatus() == 201) {
                String location = response.getHeaderString("Location");
                String keycloakId = location.substring(location.lastIndexOf('/') + 1);
                log.info("Keycloak user created: {} (ID: {})", username, keycloakId);
                return keycloakId;
            } else if (response.getStatus() == 409) {
                log.warn("Keycloak user already exists: {}", username);
                throw KeycloakErrorCode.KC_USER_CREATION_FAILED.toException(
                        "User with username '" + username + "' already exists in Keycloak");
            } else {
                String body = response.readEntity(String.class);
                log.error("Failed to create Keycloak user: {} - Status: {} Body: {}", username, response.getStatus(),
                        body);
                throw KeycloakErrorCode.KC_USER_CREATION_FAILED.toException(body);
            }
        }
    }

    @Override
    public void updateUser(String keycloakId, String email, String firstName, String lastName) {
        log.debug("Updating Keycloak user: {}", keycloakId);

        try {
            UserRepresentation userRep = usersResource().get(keycloakId).toRepresentation();
            userRep.setEmail(email);
            userRep.setFirstName(firstName);
            userRep.setLastName(lastName);
            usersResource().get(keycloakId).update(userRep);
            log.info("Keycloak user updated: {}", keycloakId);
        } catch (jakarta.ws.rs.NotFoundException e) {
            log.warn("User {} not found in Keycloak during update. Skipping Keycloak update.", keycloakId);
        } catch (Exception e) {
            log.error("Failed to update Keycloak user: {}", keycloakId, e);
            throw KeycloakErrorCode.KC_USER_UPDATE_FAILED.toException(e.getMessage());
        }
    }

    @Override
    public void setUserEnabled(String keycloakId, boolean enabled) {
        log.debug("Setting Keycloak user {} enabled={}", keycloakId, enabled);

        try {
            UserRepresentation userRep = usersResource().get(keycloakId).toRepresentation();
            userRep.setEnabled(enabled);
            usersResource().get(keycloakId).update(userRep);
            log.info("Keycloak user {} enabled={}", keycloakId, enabled);
        } catch (jakarta.ws.rs.NotFoundException e) {
            log.warn("User {} not found in Keycloak during status update. Skipping Keycloak update.", keycloakId);
        } catch (Exception e) {
            log.error("Failed to set Keycloak user enabled: {}", keycloakId, e);
            throw KeycloakErrorCode.KC_USER_UPDATE_FAILED.toException(e.getMessage());
        }
    }

    @Override
    public void deleteUser(String keycloakId) {
        log.debug("Deleting Keycloak user: {}", keycloakId);
        try {
            usersResource().get(keycloakId).remove();
            log.info("Keycloak user deleted: {}", keycloakId);
        } catch (Exception e) {
            log.error("Failed to delete Keycloak user: {}", keycloakId, e);
            // Don't throw - this is used in compensating transaction cleanup
        }
    }

    @Override
    public UserRepresentation getUserById(String keycloakId) {
        return usersResource().get(keycloakId).toRepresentation();
    }

    @Override
    public List<UserRepresentation> listUsers() {
        return usersResource().list(0, 1000);
    }

    @Override
    public List<UserRepresentation> getUsersByRealmRole(String roleName) {
        try {
            RoleRepresentation role = realmResource().roles().get(roleName).toRepresentation();
            return realmResource().roles().get(role.getName()).getUserMembers(0, 1000);
        } catch (Exception e) {
            log.warn("Failed to get users by realm role '{}': {}", roleName, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String createClient(String clientId) {
        log.debug("Creating Keycloak client: {}", clientId);
        try {
            ClientRepresentation client = new ClientRepresentation();
            client.setClientId(clientId);
            client.setName(clientId);
            client.setServiceAccountsEnabled(true);
            client.setStandardFlowEnabled(true);
            client.setPublicClient(false); // Must be false for service accounts
            client.setProtocol("openid-connect");

            try (Response response = realmResource().clients().create(client)) {
                if (response.getStatus() == 201) {
                    log.info("Keycloak client created: {}", clientId);
                    ClientRepresentation createdClient = realmResource().clients().findByClientId(clientId).get(0);
                    CredentialRepresentation secret = realmResource().clients().get(createdClient.getId()).getSecret();
                    return secret.getValue();
                } else if (response.getStatus() == 409) {
                    log.warn("Keycloak client already exists: {}", clientId);
                    ClientRepresentation existingClient = realmResource().clients().findByClientId(clientId).get(0);
                    CredentialRepresentation secret = realmResource().clients().get(existingClient.getId()).getSecret();
                    return secret.getValue();
                } else {
                    String body = response.readEntity(String.class);
                    log.error("Failed to create Keycloak client: {} - Status: {} Body: {}", clientId, response.getStatus(), body);
                    throw new RuntimeException("Failed to create client: " + body);
                }
            }
        } catch (Exception e) {
            log.error("Exception creating Keycloak client {}", clientId, e);
            throw new RuntimeException("Exception creating Keycloak client: " + e.getMessage());
        }
    }
}
