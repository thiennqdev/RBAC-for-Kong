package com.vcc.template.common.tenant;

import com.vcc.template.project.entity.ProjectStatus;
import com.vcc.template.project.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Resolves the current tenant schema from JWT token or HTTP header.
 * Implements Hibernate's CurrentTenantIdentifierResolver for multi-tenancy
 * support.
 * Excludes INACTIVE projects from tenant resolution.
 */
@Component
@Slf4j
public class TenantSchemaResolver implements CurrentTenantIdentifierResolver<String>, ApplicationContextAware {
    public static final String DEFAULT_TENANT = "public";

    private static final ThreadLocal<Boolean> loopPreventer = ThreadLocal.withInitial(() -> false);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String resolveCurrentTenantIdentifier() {
        String currentTenant = TenantContext.getCurrentTenant();

        if (Objects.nonNull(currentTenant) && !DEFAULT_TENANT.equals(currentTenant)) {
            if (loopPreventer.get()) {
                return DEFAULT_TENANT;
            }
            try {
                loopPreventer.set(true);
                // Lazy lookup to avoid circular dependency
                ProjectRepository projectRepository = applicationContext.getBean(ProjectRepository.class);

                // Check if the project exists and is ACTIVE
                return projectRepository.findByProjectCode(currentTenant)
                        .filter(project -> ProjectStatus.ACTIVE.equals(project.getStatus()))
                        .map(project -> {
                            // Normalize schema name to lowercase to match PostgreSQL behavior
                            String normalizedTenant = currentTenant.toLowerCase();
                            log.debug("Resolved active tenant: {} (normalized to: {})", currentTenant,
                                    normalizedTenant);
                            return normalizedTenant;
                        })
                        .orElseGet(() -> {
                            log.warn("Tenant '{}' is either not found or INACTIVE, using default schema: {}",
                                    currentTenant, DEFAULT_TENANT);
                            return DEFAULT_TENANT;
                        });
            } catch (Exception e) {
                // If there's any issue accessing the repository (e.g., during startup), fall
                // back to default
                log.warn("Unable to check project status for tenant '{}', using default schema: {}. Error: {}",
                        currentTenant, DEFAULT_TENANT, e.getMessage());
                return DEFAULT_TENANT;
            } finally {
                loopPreventer.remove();
            }
        } else {
            log.debug("No tenant in context or using default tenant, using default schema: {}", DEFAULT_TENANT);
            return DEFAULT_TENANT;
        }
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
