package com.vcc.template.kong.service;

import com.vcc.template.common.exception.BusinessException;
import com.vcc.template.project.errorcode.ProjectErrorCode;
import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.dto.KongTargetDto;
import com.vcc.template.kong.dto.KongTargetRequest;
import com.vcc.template.kong.entity.KongTargetEntity;
import com.vcc.template.kong.entity.KongUpstreamEntity;
import com.vcc.template.kong.entity.SyncStatus;
import com.vcc.template.kong.mapper.KongMapper;
import com.vcc.template.kong.repository.KongTargetRepository;
import com.vcc.template.kong.repository.KongUpstreamRepository;
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
public class KongTargetService {

    private final KongTargetRepository kongTargetRepository;
    private final KongUpstreamRepository kongUpstreamRepository;
    private final ProjectRepository projectRepository;
    private final KongInstanceRepository kongInstanceRepository;
    private final KongAdminClient kongAdminClient;
    private final KongMapper kongMapper = KongMapper.INSTANCE;

    @Transactional(readOnly = true)
    public List<KongTargetDto> getTargets(Long kongInstanceId, String projectCode, Long upstreamId) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));

        // Validate upstream belongs to project
        KongUpstreamEntity upstream = kongUpstreamRepository.findByIdAndProjectIdAndKongInstanceId(upstreamId, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Upstream not found: " + upstreamId));

        List<KongTargetDto> dtos = kongTargetRepository.findByKongUpstreamIdAndProjectIdAndKongInstanceId(upstreamId, project.getId(), kongInstanceId)
                .stream()
                .map(kongMapper::toDto)
                .collect(Collectors.toList());
                
        try {
            Map healthResponse = kongAdminClient.getUpstreamHealth(kongInstanceId, upstream.getKongId());
            if (healthResponse != null && healthResponse.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) healthResponse.get("data");
                Map<String, String> healthMap = data.stream()
                        .collect(Collectors.toMap(
                                d -> (String) d.get("id"),
                                d -> (String) d.get("health"),
                                (existing, replacement) -> existing
                        ));
                for (KongTargetDto dto : dtos) {
                    if (healthMap.containsKey(dto.getKongId())) {
                        dto.setHealth(healthMap.get(dto.getKongId()));
                    } else {
                        dto.setHealth("HEALTHCHECKS_OFF");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch upstream health from Kong for upstream {}", upstream.getKongId(), e);
        }

        return dtos;
    }

    @Transactional
    public KongTargetDto addTarget(Long kongInstanceId, String projectCode, Long upstreamId, KongTargetRequest request) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));

        KongUpstreamEntity upstream = kongUpstreamRepository.findByIdAndProjectIdAndKongInstanceId(upstreamId, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Upstream not found: " + upstreamId));

        String kongId = UUID.randomUUID().toString();

        // 1. Save to DB first (flush to validate constraints immediately)
        KongTargetEntity entity = new KongTargetEntity();
        entity.setKongId(kongId);
        entity.setKongUpstream(upstream);
        entity.setProject(project);
        KongInstanceEntity kongInstance = kongInstanceRepository.findById(kongInstanceId).orElseThrow(() -> new IllegalArgumentException("Kong Instance not found"));
        entity.setKongInstance(kongInstance);
        entity.setTarget(request.getTarget());
        entity.setWeight(request.getWeight());
        entity.setSyncStatus(SyncStatus.SYNCED);
        entity.setTags(request.getTags());
        kongTargetRepository.saveAndFlush(entity);

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", kongId);
        payload.put("target", request.getTarget());
        payload.put("weight", request.getWeight());
        if (request.getTags() != null) payload.put("tags", request.getTags());
        kongAdminClient.addTarget(kongInstanceId, upstream.getKongId(), payload);

        return kongMapper.toDto(entity);
    }

    @Transactional
    public void deleteTarget(Long kongInstanceId, String projectCode, Long upstreamId, Long targetId) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));

        KongUpstreamEntity upstream = kongUpstreamRepository.findByIdAndProjectIdAndKongInstanceId(upstreamId, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Upstream not found: " + upstreamId));

        KongTargetEntity target = kongTargetRepository.findByIdAndProjectIdAndKongInstanceId(targetId, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Target not found: " + targetId));

        String upstreamKongId = upstream.getKongId();
        String targetKongId = target.getKongId();

        // 1. Delete from DB first (flush to execute SQL immediately)
        kongTargetRepository.delete(target);
        kongTargetRepository.flush();

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        kongAdminClient.deleteTarget(kongInstanceId, upstreamKongId, targetKongId);
    }
}
