package com.vcc.template.project.schema;

import com.vcc.template.project.entity.Project;
import com.vcc.template.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationInitializer {

    private final ProjectRepository projectRepository;
    private final LiquibaseMigrationRunner liquibaseMigrationRunner;

    @EventListener(ApplicationReadyEvent.class)
    public void runTenantMigrations() {
        log.info("Starting schema migrations for all existing tenants...");
        List<Project> projects = projectRepository.findAll();
        for (Project project : projects) {
            try {
                liquibaseMigrationRunner.runMigrations(project.getProjectCode());
            } catch (Exception e) {
                log.error("Failed to run migration for tenant: {}", project.getProjectCode(), e);
            }
        }
        log.info("Finished schema migrations for all tenants.");
    }
}
