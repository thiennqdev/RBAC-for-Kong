package com.vcc.template.kong.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.entity.KongServiceEntity;
import com.vcc.template.kong.entity.SyncStatus;
import com.vcc.template.kong.repository.KongServiceRepository;
import com.vcc.template.project.entity.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for toggling Kong Gateway resources when a project
 * is soft-deleted (INACTIVE) or reactivated (ACTIVE).
 *
 * <p><b>Deactivation strategy:</b> For each Kong Service that is currently enabled,
 * set {@code enabled = false} and add a sentinel tag {@code "system:soft-deleted"}
 * so the system can distinguish "disabled by soft-delete" from "disabled by user".</p>
 *
 * <p><b>Reactivation strategy:</b> Find all Kong Services with the sentinel tag,
 * remove the tag and set {@code enabled = true}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KongProjectStateService {

    private static final String SOFT_DELETED_TAG = "system:soft-deleted";

    private final KongServiceRepository kongServiceRepository;
    private final KongAdminClient kongAdminClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Disables all active Kong Services for the given project.
     * Only Services that are currently {@code enabled = true} will be touched.
     * A sentinel tag is added so reactivation knows which ones to restore.
     *
     * @param project the project being soft-deleted
     */
    @Transactional
    public void deactivateProject(Project project) {
        String projectCode = project.getProjectCode();
        log.info("Deactivating Kong resources for project: {}", projectCode);

        List<KongServiceEntity> services = kongServiceRepository.findByProjectId(project.getId());
        int disabledCount = 0;

        for (KongServiceEntity service : services) {
            // Only disable services that are currently enabled
            if (Boolean.TRUE.equals(service.getEnabled())) {
                // 1. Add sentinel tag to the entity's tag list
                List<String> tags = parseTags(service.getTags());
                if (!tags.contains(SOFT_DELETED_TAG)) {
                    tags.add(SOFT_DELETED_TAG);
                }

                // 2. Update local DB
                service.setEnabled(false);
                service.setTags(serializeTags(tags));
                service.setSyncStatus(SyncStatus.SYNCED);
                kongServiceRepository.save(service);

                // 3. Sync to Kong Gateway
                Map<String, Object> payload = new HashMap<>();
                payload.put("enabled", false);
                payload.put("tags", tags);
                try {
                    kongAdminClient.updateService(service.getKongInstance().getId(), projectCode, service.getKongId(), payload);
                    disabledCount++;
                    log.debug("Disabled Kong Service: {} (kongId={})", service.getName(), service.getKongId());
                } catch (Exception e) {
                    log.error("Failed to disable Kong Service {} on Gateway: {}",
                            service.getKongId(), e.getMessage(), e);
                    throw new RuntimeException(
                            "Failed to deactivate Kong Service " + service.getKongId(), e);
                }
            }
        }

        log.info("Deactivation complete for project {}. Disabled {} service(s).", projectCode, disabledCount);
    }

    /**
     * Re-enables all Kong Services that were previously disabled by soft-delete.
     * Only Services carrying the sentinel tag {@code "system:soft-deleted"} will be restored.
     *
     * @param project the project being reactivated
     */
    @Transactional
    public void reactivateProject(Project project) {
        String projectCode = project.getProjectCode();
        log.info("Reactivating Kong resources for project: {}", projectCode);

        List<KongServiceEntity> services = kongServiceRepository.findByProjectId(project.getId());
        int enabledCount = 0;

        for (KongServiceEntity service : services) {
            List<String> tags = parseTags(service.getTags());

            // Only re-enable services that were disabled by soft-delete
            if (tags.contains(SOFT_DELETED_TAG)) {
                // 1. Remove sentinel tag
                tags.remove(SOFT_DELETED_TAG);

                // 2. Update local DB
                service.setEnabled(true);
                service.setTags(serializeTags(tags));
                service.setSyncStatus(SyncStatus.SYNCED);
                kongServiceRepository.save(service);

                // 3. Sync to Kong Gateway
                Map<String, Object> payload = new HashMap<>();
                payload.put("enabled", true);
                payload.put("tags", tags);
                try {
                    kongAdminClient.updateService(service.getKongInstance().getId(), projectCode, service.getKongId(), payload);
                    enabledCount++;
                    log.debug("Re-enabled Kong Service: {} (kongId={})", service.getName(), service.getKongId());
                } catch (Exception e) {
                    log.error("Failed to re-enable Kong Service {} on Gateway: {}",
                            service.getKongId(), e.getMessage(), e);
                    throw new RuntimeException(
                            "Failed to reactivate Kong Service " + service.getKongId(), e);
                }
            }
        }

        log.info("Reactivation complete for project {}. Re-enabled {} service(s).", projectCode, enabledCount);
    }

    // ─── Tag helpers ───────────────────────────────────────────────────

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(tagsJson, new TypeReference<List<String>>() {}));
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse tags JSON: {}", tagsJson, e);
            return new ArrayList<>();
        }
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize tags: {}", tags, e);
            return null;
        }
    }
}
