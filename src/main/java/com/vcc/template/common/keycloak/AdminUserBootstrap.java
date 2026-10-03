package com.vcc.template.common.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bootstraps admin users from Keycloak into the local database on application
 * startup.
 * Syncs users with the SYSTEM_ADMIN realm role so they exist in local DB.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminUserBootstrap implements ApplicationRunner {

    private final KeycloakAdminService keycloakAdminService;
    private final AdminUserSyncService adminUserSyncService;

    @Value("${app.bootstrap.sync-admin-users:true}")
    private boolean syncEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!syncEnabled) {
            log.info("Admin user sync is disabled (app.bootstrap.sync-admin-users=false)");
            return;
        }

        log.info("Starting admin user sync from Keycloak...");

        try {
            List<UserRepresentation> adminUsers = keycloakAdminService.getUsersByRealmRole("SYSTEM_ADMIN");

            if (adminUsers.isEmpty()) {
                log.warn("No users with SYSTEM_ADMIN role found in Keycloak");
                return;
            }

            int synced = 0;
            for (UserRepresentation kcUser : adminUsers) {
                try {
                    boolean result = adminUserSyncService.syncUser(kcUser);
                    if (result)
                        synced++;
                } catch (Exception e) {
                    log.error("Failed to sync user {}: {}", kcUser.getUsername(), e.getMessage());
                }
            }

            log.info("Admin user sync complete. Synced/Updated {} users out of {} total SYSTEM_ADMIN users.",
                    synced, adminUsers.size());
        } catch (Exception e) {
            log.error("Failed to sync admin users from Keycloak. Application will continue without sync.", e);
        }
    }
}
