package com.vcc.template.kong.service;

import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.repository.*;
import com.vcc.template.project.entity.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for cleaning up all Kong Gateway configurations
 * associated with a project. Used during hard delete to prevent orphaned
 * resources on Kong.
 *
 * Deletion order: Plugins → Routes → Services → Upstreams
 * This order respects Kong's referential integrity constraints.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KongProjectCleanupService {

    private final KongAdminClient kongAdminClient;
    private final KongPluginRepository pluginRepository;
    private final KongTargetRepository targetRepository;
    private final KongRouteRepository routeRepository;
    private final KongServiceRepository serviceRepository;
    private final KongUpstreamRepository upstreamRepository;
    private final KongInstanceRepository kongInstanceRepository;
    private final KongJwtProvisioningService kongJwtProvisioningService;

    /**
     * Deletes all Kong resources (plugins, routes, services, upstreams)
     * tagged with the given projectCode.
     *
     * @param project the project whose Kong resources should be purged
     * @throws RuntimeException if any Kong API call fails, allowing the
     *                          caller's transaction to roll back
     */
    @Transactional
    public void cleanupProject(Project project) {
        String projectCode = project.getProjectCode();
        Long projectId = project.getId();
        log.warn("Starting Kong cleanup for project: {}", projectCode);

        // 1-4. Delete from Kong Gateway (iterate all instances)
        List<com.vcc.template.kong.entity.KongInstanceEntity> instances = kongInstanceRepository.findAll();
        for (com.vcc.template.kong.entity.KongInstanceEntity instance : instances) {
            Long kongInstanceId = instance.getId();
            try {
                deleteAllPlugins(kongInstanceId, projectCode);
                deleteAllRoutes(kongInstanceId, projectCode);
                deleteAllServices(kongInstanceId, projectCode);
                deleteAllUpstreams(kongInstanceId, projectCode);
            } catch (Exception e) {
                log.error("Failed to cleanup Kong resources for instance {} project {}: {}", kongInstanceId, projectCode, e.getMessage());
            }

            // 4.5. Deprovision JWT Consumer for this tenant on this instance
            try {
                kongJwtProvisioningService.deprovisionJwtForProject(kongInstanceId, projectCode);
            } catch (Exception e) {
                log.warn("Failed to deprovision JWT for instance {} project {}, continuing cleanup: {}", kongInstanceId, projectCode, e.getMessage());
            }
        }

        // 5. Delete local database records from public schema (in dependency-safe order)
        log.info("Cleaning up local database Kong records for project: {}", projectCode);
        pluginRepository.deleteAllByProjectId(projectId);
        targetRepository.deleteAllByProjectId(projectId);
        routeRepository.deleteAllByProjectId(projectId);
        serviceRepository.deleteAllByProjectId(projectId);
        upstreamRepository.deleteAllByProjectId(projectId);

        log.warn("Kong cleanup completed for project: {}", projectCode);
    }

    private void deleteAllPlugins(Long kongInstanceId, String projectCode) {
        log.info("Cleaning up Kong plugins for project: {} on instance: {}", projectCode, kongInstanceId);
        List<Map<String, Object>> plugins = fetchDataList(
                kongAdminClient.listPlugins(kongInstanceId, projectCode));

        for (Map<String, Object> plugin : plugins) {
            String id = (String) plugin.get("id");
            try {
                kongAdminClient.deletePlugin(kongInstanceId, projectCode, id);
                log.info("Deleted Kong plugin: {}", id);
            } catch (Exception e) {
                log.error("Failed to delete Kong plugin {}: {}", id, e.getMessage(), e);
                throw new RuntimeException(
                        "Kong cleanup failed: could not delete plugin " + id, e);
            }
        }
    }

    private void deleteAllRoutes(Long kongInstanceId, String projectCode) {
        log.info("Cleaning up Kong routes for project: {} on instance: {}", projectCode, kongInstanceId);
        List<Map<String, Object>> routes = fetchDataList(
                kongAdminClient.listAllRoutes(kongInstanceId, projectCode));

        for (Map<String, Object> route : routes) {
            String id = (String) route.get("id");
            try {
                kongAdminClient.deleteRoute(kongInstanceId, projectCode, id);
                log.info("Deleted Kong route: {}", id);
            } catch (Exception e) {
                log.error("Failed to delete Kong route {}: {}", id, e.getMessage(), e);
                throw new RuntimeException(
                        "Kong cleanup failed: could not delete route " + id, e);
            }
        }
    }

    private void deleteAllServices(Long kongInstanceId, String projectCode) {
        log.info("Cleaning up Kong services for project: {} on instance: {}", projectCode, kongInstanceId);
        List<Map<String, Object>> services = fetchDataList(
                kongAdminClient.listServices(kongInstanceId, projectCode));

        for (Map<String, Object> service : services) {
            String id = (String) service.get("id");
            try {
                kongAdminClient.deleteService(kongInstanceId, projectCode, id);
                log.info("Deleted Kong service: {}", id);
            } catch (Exception e) {
                log.error("Failed to delete Kong service {}: {}", id, e.getMessage(), e);
                throw new RuntimeException(
                        "Kong cleanup failed: could not delete service " + id, e);
            }
        }
    }

    private void deleteAllUpstreams(Long kongInstanceId, String projectCode) {
        log.info("Cleaning up Kong upstreams for project: {} on instance: {}", projectCode, kongInstanceId);
        List<Map<String, Object>> upstreams = fetchDataList(
                kongAdminClient.listUpstreams(kongInstanceId, projectCode));

        for (Map<String, Object> upstream : upstreams) {
            String id = (String) upstream.get("id");
            try {
                kongAdminClient.deleteUpstream(kongInstanceId, projectCode, id);
                log.info("Deleted Kong upstream: {}", id);
            } catch (Exception e) {
                log.error("Failed to delete Kong upstream {}: {}", id, e.getMessage(), e);
                throw new RuntimeException(
                        "Kong cleanup failed: could not delete upstream " + id, e);
            }
        }
    }

    /**
     * Extracts the "data" array from a Kong Admin API list response.
     * Kong returns { "data": [...], "next": "..." }.
     *
     * @param response the raw response Map from KongAdminClient
     * @return the list of entity maps, or empty list if none
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchDataList(Map response) {
        if (response == null || !response.containsKey("data")) {
            return List.of();
        }
        Object data = response.get("data");
        if (data instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }
}
