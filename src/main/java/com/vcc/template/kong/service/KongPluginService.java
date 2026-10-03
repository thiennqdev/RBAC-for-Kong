package com.vcc.template.kong.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vcc.template.common.exception.BusinessException;
import com.vcc.template.project.errorcode.ProjectErrorCode;import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.dto.KongPluginDto;
import com.vcc.template.kong.dto.KongPluginRequest;
import com.vcc.template.kong.entity.KongPluginEntity;
import com.vcc.template.kong.entity.KongRouteEntity;
import com.vcc.template.kong.entity.KongServiceEntity;
import com.vcc.template.kong.entity.SyncStatus;
import com.vcc.template.kong.mapper.KongMapper;
import com.vcc.template.kong.repository.KongPluginRepository;
import com.vcc.template.kong.repository.KongRouteRepository;
import com.vcc.template.kong.repository.KongServiceRepository;
import com.vcc.template.project.entity.Project;
import com.vcc.template.project.repository.ProjectRepository;
import com.vcc.template.kong.repository.KongInstanceRepository;
import com.vcc.template.kong.entity.KongInstanceEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KongPluginService {

    private final KongPluginRepository kongPluginRepository;
    private final KongServiceRepository kongServiceRepository;
    private final KongRouteRepository kongRouteRepository;
    private final ProjectRepository projectRepository;
    private final KongInstanceRepository kongInstanceRepository;
    private final KongAdminClient kongAdminClient;
    private final KongMapper kongMapper = KongMapper.INSTANCE;

    @org.springframework.beans.factory.annotation.Value("${kong.http-log.callback-url}")
    private String httpLogCallbackUrl;

    @Transactional(readOnly = true)
    public List<KongPluginDto> getPlugins(Long kongInstanceId, String projectCode) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        return kongPluginRepository.findByProjectIdAndKongInstanceId(project.getId(), kongInstanceId)
                .stream()
                .map(kongMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public KongPluginDto createPlugin(Long kongInstanceId, String projectCode, KongPluginRequest request) {
        if ("GLOBAL".equalsIgnoreCase(request.getScope())) {
            throw new IllegalArgumentException("Global plugins are not allowed for tenants.");
        }

        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));

        String kongId = UUID.randomUUID().toString();

        // Resolve scope entity for Kong payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", kongId);
        payload.put("name", request.getPluginName());
        if (request.getConfig() != null) payload.put("config", request.getConfig());
        if (request.getEnabled() != null) payload.put("enabled", request.getEnabled());

        if ("SERVICE".equalsIgnoreCase(request.getScope())) {
            KongServiceEntity serviceEntity = kongServiceRepository.findByIdAndProjectIdAndKongInstanceId(request.getScopeEntityId(), project.getId(), kongInstanceId)
                    .orElseThrow(() -> new IllegalArgumentException("Service not found: " + request.getScopeEntityId()));
            Map<String, Object> serviceRef = new HashMap<>();
            serviceRef.put("id", serviceEntity.getKongId());
            payload.put("service", serviceRef);
        } else if ("ROUTE".equalsIgnoreCase(request.getScope())) {
            KongRouteEntity routeEntity = kongRouteRepository.findByIdAndProjectIdAndKongInstanceId(request.getScopeEntityId(), project.getId(), kongInstanceId)
                    .orElseThrow(() -> new IllegalArgumentException("Route not found: " + request.getScopeEntityId()));
            Map<String, Object> routeRef = new HashMap<>();
            routeRef.put("id", routeEntity.getKongId());
            payload.put("route", routeRef);
        } else {
            throw new IllegalArgumentException("Invalid scope: " + request.getScope());
        }

        // 1. Save to DB first (flush to validate constraints immediately)
        KongPluginEntity entity = new KongPluginEntity();
        entity.setKongId(kongId);
        entity.setProject(project);
        KongInstanceEntity kongInstance = kongInstanceRepository.findById(kongInstanceId).orElseThrow(() -> new IllegalArgumentException("Kong Instance not found"));
        entity.setKongInstance(kongInstance);
        entity.setPluginName(request.getPluginName());
        entity.setScope(request.getScope().toUpperCase());
        entity.setScopeEntityId(request.getScopeEntityId());
        entity.setConfig(kongMapper.mapToJson(request.getConfig()));
        entity.setEnabled(request.getEnabled());
        entity.setSyncStatus(SyncStatus.SYNCED);
        kongPluginRepository.saveAndFlush(entity);

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        kongAdminClient.createPlugin(kongInstanceId, projectCode, payload);

        return kongMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public KongPluginDto getPlugin(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongPluginEntity entity = kongPluginRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + id));
        return kongMapper.toDto(entity);
    }

    @Transactional
    public KongPluginDto updatePlugin(Long kongInstanceId, String projectCode, Long id, KongPluginRequest request) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongPluginEntity entity = kongPluginRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + id));

        // 1. Update DB first (flush to validate constraints immediately)
        entity.setConfig(kongMapper.mapToJson(request.getConfig()));
        entity.setEnabled(request.getEnabled());
        entity.setSyncStatus(SyncStatus.SYNCED);
        kongPluginRepository.saveAndFlush(entity);

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        // We do not allow changing scope or scope_entity_id during update. Kong does not either easily.
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", request.getPluginName()); // Technically plugin name might not change
        if (request.getConfig() != null) payload.put("config", request.getConfig());
        if (request.getEnabled() != null) payload.put("enabled", request.getEnabled());
        kongAdminClient.updatePlugin(kongInstanceId, projectCode, entity.getKongId(), payload);

        return kongMapper.toDto(entity);
    }

    @Transactional
    public void deletePlugin(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongPluginEntity entity = kongPluginRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + id));

        String kongIdToDelete = entity.getKongId();

        // 1. Delete from DB first (flush to execute SQL immediately)
        kongPluginRepository.delete(entity);
        kongPluginRepository.flush();

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        kongAdminClient.deletePlugin(kongInstanceId, projectCode, kongIdToDelete);
    }

    /**
     * Enables the http-log plugin on a Kong Service or Route for a given project.
     *
     * <p>Kong OSS does not include the kafka-log plugin (Enterprise only).
     * Instead, we use the built-in http-log plugin to POST request/response logs
     * to our backend endpoint, which then bridges them into Kafka for the
     * Live Log Analytics pipeline.</p>
     *
     * <p>The log messages are tagged with the project_code so that downstream
     * consumers (KafkaLogConsumer) can filter logs per tenant/project.</p>
     *
     * @param projectCode the project code used for tagging
     * @param entityType  "services" or "routes"
     * @param entityKongId the Kong ID of the service or route
     */
    public void enableHttpLogPlugin(Long kongInstanceId, String projectCode, String entityType, String entityKongId) {
        log.info("Enabling http-log plugin on {} {} for project {}", entityType, entityKongId, projectCode);

        Map<String, Object> pluginPayload = new HashMap<>();
        pluginPayload.put("name", "http-log");
        pluginPayload.put("tags", List.of("project_code:" + projectCode, "auto-provisioned"));

        Map<String, Object> config = new HashMap<>();
        config.put("http_endpoint", httpLogCallbackUrl);
        config.put("method", "POST");
        config.put("timeout", 10000);
        config.put("keepalive", 60000);
        config.put("content_type", "application/json");
        pluginPayload.put("config", config);

        // Attach to specific entity
        Map<String, Object> entityRef = new HashMap<>();
        entityRef.put("id", entityKongId);
        if ("services".equalsIgnoreCase(entityType)) {
            pluginPayload.put("service", entityRef);
        } else if ("routes".equalsIgnoreCase(entityType)) {
            pluginPayload.put("route", entityRef);
        }

        try {
            kongAdminClient.createPlugin(kongInstanceId, projectCode, pluginPayload);
            log.info("http-log plugin enabled on {} {} for project {}", entityType, entityKongId, projectCode);
        } catch (Exception e) {
            log.warn("Failed to enable http-log plugin on {} {} for project {}: {}",
                    entityType, entityKongId, projectCode, e.getMessage());
            // Don't throw — http-log is supplementary, should not block service creation
        }
    }
}
