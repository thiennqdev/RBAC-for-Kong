package com.vcc.template.kong.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vcc.template.common.exception.BusinessException;
import com.vcc.template.project.errorcode.ProjectErrorCode;import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.dto.KongUpstreamDto;
import com.vcc.template.kong.dto.KongUpstreamRequest;
import com.vcc.template.kong.entity.KongUpstreamEntity;
import com.vcc.template.kong.entity.SyncStatus;
import com.vcc.template.kong.mapper.KongMapper;
import com.vcc.template.kong.naming.NamingConventionEnforcer;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class KongUpstreamService {

    private final KongUpstreamRepository kongUpstreamRepository;
    private final ProjectRepository projectRepository;
    private final KongInstanceRepository kongInstanceRepository;
    private final KongAdminClient kongAdminClient;
    private final NamingConventionEnforcer namingEnforcer;
    private final KongMapper kongMapper = KongMapper.INSTANCE;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<KongUpstreamDto> getUpstreams(Long kongInstanceId, String projectCode) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        return kongUpstreamRepository.findByProjectIdAndKongInstanceId(project.getId(), kongInstanceId)
                .stream()
                .map(this::mapToDtoWithHealthchecks)
                .collect(Collectors.toList());
    }

    @Transactional
    public KongUpstreamDto createUpstream(Long kongInstanceId, String projectCode, KongUpstreamRequest request) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));

        String fullName = namingEnforcer.formatName(projectCode, request.getName());
        String kongId = UUID.randomUUID().toString();

        // 1. Save to DB first (flush to validate constraints immediately)
        KongUpstreamEntity entity = new KongUpstreamEntity();
        entity.setKongId(kongId);
        entity.setProject(project);
        KongInstanceEntity kongInstance = kongInstanceRepository.findById(kongInstanceId).orElseThrow(() -> new IllegalArgumentException("Kong Instance not found"));
        entity.setKongInstance(kongInstance);
        entity.setName(fullName);
        entity.setDisplayName(request.getName());
        entity.setAlgorithm(request.getAlgorithm());
        entity.setSlots(request.getSlots());
        entity.setSyncStatus(SyncStatus.SYNCED);
        
        entity.setHealthchecks(request.getHealthchecks());
        entity.setHashOn(request.getHashOn());
        entity.setHashFallback(request.getHashFallback());
        entity.setHashOnHeader(request.getHashOnHeader());
        entity.setHashFallbackHeader(request.getHashFallbackHeader());
        entity.setHashOnCookie(request.getHashOnCookie());
        entity.setHashOnCookiePath(request.getHashOnCookiePath());
        entity.setHashOnQueryArg(request.getHashOnQueryArg());
        entity.setHashFallbackQueryArg(request.getHashFallbackQueryArg());
        entity.setHashOnUriCapture(request.getHashOnUriCapture());
        entity.setHashFallbackUriCapture(request.getHashFallbackUriCapture());
        entity.setHostHeader(request.getHostHeader());
        entity.setClientCertificate(request.getClientCertificate());
        entity.setUseSrvName(request.getUseSrvName());
        entity.setTags(request.getTags());
        
        kongUpstreamRepository.saveAndFlush(entity);

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", kongId);
        payload.put("name", fullName);
        if (request.getAlgorithm() != null) payload.put("algorithm", request.getAlgorithm());
        if (request.getSlots() != null) payload.put("slots", request.getSlots());
        if (request.getHealthchecks() != null) payload.put("healthchecks", request.getHealthchecks());
        if (request.getHashOn() != null) payload.put("hash_on", request.getHashOn());
        if (request.getHashFallback() != null) payload.put("hash_fallback", request.getHashFallback());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnHeader())) payload.put("hash_on_header", request.getHashOnHeader());
        if (org.springframework.util.StringUtils.hasText(request.getHashFallbackHeader())) payload.put("hash_fallback_header", request.getHashFallbackHeader());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnCookie())) payload.put("hash_on_cookie", request.getHashOnCookie());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnCookiePath())) payload.put("hash_on_cookie_path", request.getHashOnCookiePath());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnQueryArg())) payload.put("hash_on_query_arg", request.getHashOnQueryArg());
        if (org.springframework.util.StringUtils.hasText(request.getHashFallbackQueryArg())) payload.put("hash_fallback_query_arg", request.getHashFallbackQueryArg());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnUriCapture())) payload.put("hash_on_uri_capture", request.getHashOnUriCapture());
        if (org.springframework.util.StringUtils.hasText(request.getHashFallbackUriCapture())) payload.put("hash_fallback_uri_capture", request.getHashFallbackUriCapture());
        if (org.springframework.util.StringUtils.hasText(request.getHostHeader())) payload.put("host_header", request.getHostHeader());
        if (org.springframework.util.StringUtils.hasText(request.getClientCertificate())) payload.put("client_certificate", Map.of("id", request.getClientCertificate()));
        if (request.getUseSrvName() != null) payload.put("use_srv_name", request.getUseSrvName());
        if (request.getTags() != null) payload.put("tags", request.getTags());
        kongAdminClient.createUpstream(kongInstanceId, projectCode, payload);

        return mapToDtoWithHealthchecks(entity);
    }

    @Transactional(readOnly = true)
    public KongUpstreamDto getUpstream(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongUpstreamEntity entity = kongUpstreamRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Upstream not found: " + id));
        return mapToDtoWithHealthchecks(entity);
    }

    @Transactional
    public KongUpstreamDto updateUpstream(Long kongInstanceId, String projectCode, Long id, KongUpstreamRequest request) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongUpstreamEntity entity = kongUpstreamRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Upstream not found: " + id));

        String fullName = namingEnforcer.formatName(projectCode, request.getName());

        // 1. Update DB first (flush to validate constraints immediately)
        entity.setName(fullName);
        entity.setDisplayName(request.getName());
        entity.setAlgorithm(request.getAlgorithm());
        entity.setSlots(request.getSlots());
        entity.setSyncStatus(SyncStatus.SYNCED);
        
        entity.setHealthchecks(request.getHealthchecks());
        entity.setHashOn(request.getHashOn());
        entity.setHashFallback(request.getHashFallback());
        entity.setHashOnHeader(request.getHashOnHeader());
        entity.setHashFallbackHeader(request.getHashFallbackHeader());
        entity.setHashOnCookie(request.getHashOnCookie());
        entity.setHashOnCookiePath(request.getHashOnCookiePath());
        entity.setHashOnQueryArg(request.getHashOnQueryArg());
        entity.setHashFallbackQueryArg(request.getHashFallbackQueryArg());
        entity.setHashOnUriCapture(request.getHashOnUriCapture());
        entity.setHashFallbackUriCapture(request.getHashFallbackUriCapture());
        entity.setHostHeader(request.getHostHeader());
        entity.setClientCertificate(request.getClientCertificate());
        entity.setUseSrvName(request.getUseSrvName());
        entity.setTags(request.getTags());
        
        kongUpstreamRepository.saveAndFlush(entity);

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", fullName);
        if (request.getAlgorithm() != null) payload.put("algorithm", request.getAlgorithm());
        if (request.getSlots() != null) payload.put("slots", request.getSlots());
        if (request.getHealthchecks() != null) payload.put("healthchecks", request.getHealthchecks());
        if (request.getHashOn() != null) payload.put("hash_on", request.getHashOn());
        if (request.getHashFallback() != null) payload.put("hash_fallback", request.getHashFallback());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnHeader())) payload.put("hash_on_header", request.getHashOnHeader());
        if (org.springframework.util.StringUtils.hasText(request.getHashFallbackHeader())) payload.put("hash_fallback_header", request.getHashFallbackHeader());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnCookie())) payload.put("hash_on_cookie", request.getHashOnCookie());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnCookiePath())) payload.put("hash_on_cookie_path", request.getHashOnCookiePath());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnQueryArg())) payload.put("hash_on_query_arg", request.getHashOnQueryArg());
        if (org.springframework.util.StringUtils.hasText(request.getHashFallbackQueryArg())) payload.put("hash_fallback_query_arg", request.getHashFallbackQueryArg());
        if (org.springframework.util.StringUtils.hasText(request.getHashOnUriCapture())) payload.put("hash_on_uri_capture", request.getHashOnUriCapture());
        if (org.springframework.util.StringUtils.hasText(request.getHashFallbackUriCapture())) payload.put("hash_fallback_uri_capture", request.getHashFallbackUriCapture());
        if (org.springframework.util.StringUtils.hasText(request.getHostHeader())) payload.put("host_header", request.getHostHeader());
        if (org.springframework.util.StringUtils.hasText(request.getClientCertificate())) payload.put("client_certificate", Map.of("id", request.getClientCertificate()));
        if (request.getUseSrvName() != null) payload.put("use_srv_name", request.getUseSrvName());
        if (request.getTags() != null) payload.put("tags", request.getTags());
        kongAdminClient.updateUpstream(kongInstanceId, projectCode, entity.getKongId(), payload);

        return mapToDtoWithHealthchecks(entity);
    }

    @Transactional
    public void deleteUpstream(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongUpstreamEntity entity = kongUpstreamRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Upstream not found: " + id));

        String kongIdToDelete = entity.getKongId();

        // 1. Delete from DB first (flush to execute SQL immediately)
        kongUpstreamRepository.delete(entity);
        kongUpstreamRepository.flush();

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        kongAdminClient.deleteUpstream(kongInstanceId, projectCode, kongIdToDelete);
    }
    
    private KongUpstreamDto mapToDtoWithHealthchecks(KongUpstreamEntity entity) {
        KongUpstreamDto dto = kongMapper.toDto(entity);
        if (entity.getHealthchecks() != null) {
            dto.setHealthchecks(entity.getHealthchecks());
        } else if (entity.getHealthchecksConfig() != null) {
            try {
                dto.setHealthchecks(objectMapper.readValue(entity.getHealthchecksConfig(), Map.class));
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize healthchecks config", e);
            }
        }
        return dto;
    }
}
