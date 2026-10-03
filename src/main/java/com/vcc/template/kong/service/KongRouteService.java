package com.vcc.template.kong.service;

import com.vcc.template.common.exception.BusinessException;
import com.vcc.template.project.errorcode.ProjectErrorCode;
import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.dto.KongRouteDto;
import com.vcc.template.kong.dto.KongRouteRequest;
import com.vcc.template.kong.entity.KongRouteEntity;
import com.vcc.template.kong.entity.KongServiceEntity;
import com.vcc.template.kong.entity.SyncStatus;
import com.vcc.template.kong.entity.RouteStatus;
import com.vcc.template.kong.mapper.KongMapper;
import com.vcc.template.kong.naming.NamingConventionEnforcer;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KongRouteService {

    private final KongRouteRepository kongRouteRepository;
    private final KongServiceRepository kongServiceRepository;
    private final ProjectRepository projectRepository;
    private final KongInstanceRepository kongInstanceRepository;
    private final KongAdminClient kongAdminClient;
    private final NamingConventionEnforcer namingEnforcer;
    private final KongMapper kongMapper = KongMapper.INSTANCE;
    private final MailNotificationService mailNotificationService;
    private final KongJwtProvisioningService kongJwtProvisioningService;

    @Transactional(readOnly = true)
    public List<KongRouteDto> getRoutes(Long kongInstanceId, String projectCode, Long serviceId) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        return kongRouteRepository.findByProjectIdAndKongServiceIdAndKongInstanceId(project.getId(), serviceId, kongInstanceId)
                .stream()
                .map(kongMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public KongRouteDto createRoute(Long kongInstanceId, String projectCode, Long serviceId, KongRouteRequest request) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongServiceEntity serviceEntity = kongServiceRepository.findByIdAndProjectIdAndKongInstanceId(serviceId, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + serviceId));

        String fullName = namingEnforcer.formatName(projectCode, request.getName());
        String kongId = UUID.randomUUID().toString();

        List<String> prefixedPaths = namingEnforcer.formatPaths(projectCode, request.getPaths());

        // Save to DB first with DRAFT status. Do NOT sync to Kong yet.
        KongRouteEntity entity = new KongRouteEntity();
        entity.setKongId(kongId);
        entity.setKongService(serviceEntity);
        entity.setProject(project);
        KongInstanceEntity kongInstance = kongInstanceRepository.findById(kongInstanceId).orElseThrow(() -> new IllegalArgumentException("Kong Instance not found"));
        entity.setKongInstance(kongInstance);
        entity.setName(fullName);
        entity.setDisplayName(request.getName());
        entity.setPaths(kongMapper.listToJson(prefixedPaths));
        entity.setMethods(kongMapper.listToJson(request.getMethods()));
        entity.setProtocols(kongMapper.listToJson(request.getProtocols()));
        entity.setStripPath(request.getStripPath());
        entity.setPreserveHost(request.getPreserveHost());
        entity.setHosts(request.getHosts());
        entity.setHeaders(request.getHeaders());
        entity.setHttpsRedirectStatus(request.getHttpsRedirectStatus());
        entity.setRegexPriority(request.getRegexPriority());
        entity.setPathHandling(request.getPathHandling());
        entity.setRequestBuffering(request.getRequestBuffering());
        entity.setResponseBuffering(request.getResponseBuffering());
        entity.setSnis(request.getSnis());
        entity.setSources(request.getSources());
        entity.setDestinations(request.getDestinations());
        entity.setTags(request.getTags());
        entity.setStatus(RouteStatus.DRAFT);
        entity.setSyncStatus(SyncStatus.PENDING);
        
        kongRouteRepository.saveAndFlush(entity);

        return kongMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public KongRouteDto getRoute(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongRouteEntity entity = kongRouteRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
        return kongMapper.toDto(entity);
    }

    @Transactional
    public KongRouteDto updateRoute(Long kongInstanceId, String projectCode, Long id, KongRouteRequest request) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongRouteEntity entity = kongRouteRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));

        String fullName = namingEnforcer.formatName(projectCode, request.getName());
        List<String> prefixedPaths = namingEnforcer.formatPaths(projectCode, request.getPaths());

        // 1. Update DB
        entity.setName(fullName);
        entity.setDisplayName(request.getName());
        entity.setPaths(kongMapper.listToJson(prefixedPaths));
        entity.setMethods(kongMapper.listToJson(request.getMethods()));
        entity.setProtocols(kongMapper.listToJson(request.getProtocols()));
        entity.setStripPath(request.getStripPath());
        entity.setPreserveHost(request.getPreserveHost());
        entity.setHosts(request.getHosts());
        entity.setHeaders(request.getHeaders());
        entity.setHttpsRedirectStatus(request.getHttpsRedirectStatus());
        entity.setRegexPriority(request.getRegexPriority());
        entity.setPathHandling(request.getPathHandling());
        entity.setRequestBuffering(request.getRequestBuffering());
        entity.setResponseBuffering(request.getResponseBuffering());
        entity.setSnis(request.getSnis());
        entity.setSources(request.getSources());
        entity.setDestinations(request.getDestinations());
        entity.setTags(request.getTags());
        
        // 2. If ACTIVE, sync to Kong. Otherwise, keep current status.
        if (entity.getStatus() == RouteStatus.ACTIVE) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", fullName);
            if (prefixedPaths != null) payload.put("paths", prefixedPaths);
            if (request.getMethods() != null) payload.put("methods", request.getMethods());
            if (request.getProtocols() != null) payload.put("protocols", request.getProtocols());
            if (request.getStripPath() != null) payload.put("strip_path", request.getStripPath());
            if (request.getPreserveHost() != null) payload.put("preserve_host", request.getPreserveHost());
            if (request.getHosts() != null) payload.put("hosts", request.getHosts());
            if (request.getHeaders() != null) payload.put("headers", request.getHeaders());
            if (request.getHttpsRedirectStatus() != null) payload.put("https_redirect_status_code", request.getHttpsRedirectStatus());
            if (request.getRegexPriority() != null) payload.put("regex_priority", request.getRegexPriority());
            if (org.springframework.util.StringUtils.hasText(request.getPathHandling())) payload.put("path_handling", request.getPathHandling());
            if (request.getRequestBuffering() != null) payload.put("request_buffering", request.getRequestBuffering());
            if (request.getResponseBuffering() != null) payload.put("response_buffering", request.getResponseBuffering());
            if (request.getSnis() != null) payload.put("snis", request.getSnis());
            if (request.getSources() != null) payload.put("sources", request.getSources());
            if (request.getDestinations() != null) payload.put("destinations", request.getDestinations());
            if (request.getTags() != null) payload.put("tags", request.getTags());
            
            kongAdminClient.updateRoute(kongInstanceId, projectCode, entity.getKongId(), payload);
            entity.setSyncStatus(SyncStatus.SYNCED);
        } else {
            entity.setSyncStatus(SyncStatus.PENDING);
        }
        
        kongRouteRepository.saveAndFlush(entity);

        return kongMapper.toDto(entity);
    }

    @Transactional
    public void deleteRoute(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongRouteEntity entity = kongRouteRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));

        String kongIdToDelete = entity.getKongId();
        boolean wasActive = entity.getStatus() == RouteStatus.ACTIVE;

        kongRouteRepository.delete(entity);
        kongRouteRepository.flush();

        if (wasActive) {
            kongAdminClient.deleteRoute(kongInstanceId, projectCode, kongIdToDelete);
        }
    }

    @Transactional
    public KongRouteDto requestApproval(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongRouteEntity entity = kongRouteRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));

        if (entity.getStatus() != RouteStatus.DRAFT && entity.getStatus() != RouteStatus.REJECTED) {
            throw new IllegalArgumentException("Only DRAFT or REJECTED routes can request approval");
        }

        entity.setStatus(RouteStatus.PENDING_APPROVAL);
        kongRouteRepository.saveAndFlush(entity);

        mailNotificationService.sendRouteApprovalRequest(kongInstanceId, projectCode, entity);

        return kongMapper.toDto(entity);
    }

    @Transactional
    public KongRouteDto approveRoute(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongRouteEntity entity = kongRouteRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));

        if (entity.getStatus() != RouteStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Route must be in PENDING_APPROVAL status to be approved");
        }

        entity.setStatus(RouteStatus.ACTIVE);
        entity.setSyncStatus(SyncStatus.SYNCED);
        kongRouteRepository.saveAndFlush(entity);

        // Sync to Kong
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", entity.getKongId());
        payload.put("name", entity.getName());
        
        List<String> paths = kongMapper.jsonToList(entity.getPaths());
        if (paths != null && !paths.isEmpty()) payload.put("paths", paths);
        
        List<String> methods = kongMapper.jsonToList(entity.getMethods());
        if (methods != null && !methods.isEmpty()) payload.put("methods", methods);
        
        List<String> protocols = kongMapper.jsonToList(entity.getProtocols());
        if (protocols != null && !protocols.isEmpty()) payload.put("protocols", protocols);
        
        if (entity.getStripPath() != null) payload.put("strip_path", entity.getStripPath());
        if (entity.getPreserveHost() != null) payload.put("preserve_host", entity.getPreserveHost());
        if (entity.getHosts() != null) payload.put("hosts", entity.getHosts());
        if (entity.getHeaders() != null) payload.put("headers", entity.getHeaders());
        if (entity.getHttpsRedirectStatus() != null) payload.put("https_redirect_status_code", entity.getHttpsRedirectStatus());
        if (entity.getRegexPriority() != null) payload.put("regex_priority", entity.getRegexPriority());
        if (org.springframework.util.StringUtils.hasText(entity.getPathHandling())) payload.put("path_handling", entity.getPathHandling());
        if (entity.getRequestBuffering() != null) payload.put("request_buffering", entity.getRequestBuffering());
        if (entity.getResponseBuffering() != null) payload.put("response_buffering", entity.getResponseBuffering());
        if (entity.getSnis() != null) payload.put("snis", entity.getSnis());
        if (entity.getSources() != null) payload.put("sources", entity.getSources());
        if (entity.getDestinations() != null) payload.put("destinations", entity.getDestinations());
        if (entity.getTags() != null) payload.put("tags", entity.getTags());
        
        kongAdminClient.createRoute(kongInstanceId, projectCode, entity.getKongService().getKongId(), payload);

        // Enable JWT plugin on the route to validate tokens against the provisioned IdP keys (works on Kong OSS)
        try {
            if (Boolean.TRUE.equals(project.getJwtAuthEnabled())) {
                kongJwtProvisioningService.enableJwtPlugin(kongInstanceId, projectCode, "routes", entity.getKongId());
                log.info("Attached JWT plugin to route {} for project {}", entity.getKongId(), projectCode);
            }
        } catch (Exception e) {
            log.error("Failed to attach JWT plugin to route {}: {}", entity.getKongId(), e.getMessage());
        }

        return kongMapper.toDto(entity);
    }

    @Transactional
    public KongRouteDto rejectRoute(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongRouteEntity entity = kongRouteRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));

        if (entity.getStatus() != RouteStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Route must be in PENDING_APPROVAL status to be rejected");
        }

        entity.setStatus(RouteStatus.REJECTED);
        kongRouteRepository.saveAndFlush(entity);

        return kongMapper.toDto(entity);
    }
}
