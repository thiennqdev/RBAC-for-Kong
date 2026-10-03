package com.vcc.template.kong.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.vcc.template.common.exception.BusinessException;
import com.vcc.template.project.errorcode.ProjectErrorCode;import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.dto.KongServiceDto;
import com.vcc.template.kong.dto.KongServiceRequest;
import com.vcc.template.kong.entity.KongServiceEntity;
import com.vcc.template.kong.entity.SyncStatus;
import com.vcc.template.kong.mapper.KongMapper;
import com.vcc.template.kong.naming.NamingConventionEnforcer;
import com.vcc.template.kong.repository.KongServiceRepository;
import com.vcc.template.project.entity.Project;
import com.vcc.template.project.repository.ProjectRepository;
import com.vcc.template.kong.repository.KongInstanceRepository;
import com.vcc.template.kong.entity.KongInstanceEntity;
import com.vcc.template.kong.service.KongPluginService;
import org.springframework.util.StringUtils;
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
public class KongServiceService {

    private final KongServiceRepository kongServiceRepository;
    private final ProjectRepository projectRepository;
    private final KongInstanceRepository kongInstanceRepository;
    private final KongAdminClient kongAdminClient;
    private final NamingConventionEnforcer namingEnforcer;
    private final KongMapper kongMapper = KongMapper.INSTANCE;
    private final KongPluginService kongPluginService;
    private final KongJwtProvisioningService kongJwtProvisioningService;

    @Transactional(readOnly = true)
    public List<KongServiceDto> getServices(Long kongInstanceId, String projectCode) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        return kongServiceRepository.findByProjectIdAndKongInstanceId(project.getId(), kongInstanceId)
                .stream()
                .map(kongMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public KongServiceDto createService(Long kongInstanceId, String projectCode, KongServiceRequest request) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));

        String fullName = namingEnforcer.formatName(projectCode, request.getName());
        String kongId = UUID.randomUUID().toString();

        // 1. Save to DB first (flush to validate constraints immediately)
        KongServiceEntity entity = new KongServiceEntity();
        entity.setKongId(kongId);
        entity.setProject(project);
        KongInstanceEntity kongInstance = kongInstanceRepository.findById(kongInstanceId).orElseThrow(() -> new IllegalArgumentException("Kong Instance not found"));
        entity.setKongInstance(kongInstance);
        entity.setName(fullName);
        entity.setDisplayName(request.getName());
        entity.setHost(request.getHost());
        entity.setPort(request.getPort());
        entity.setProtocol(request.getProtocol());
        entity.setPath(StringUtils.hasText(request.getPath()) ? request.getPath().trim() : null);
        entity.setEnabled(request.getEnabled());
        entity.setSyncStatus(SyncStatus.SYNCED);
        entity.setTags(kongMapper.listToJson(request.getTags()));
        entity.setRetries(request.getRetries());
        entity.setConnectTimeout(request.getConnectTimeout());
        entity.setWriteTimeout(request.getWriteTimeout());
        entity.setReadTimeout(request.getReadTimeout());
        entity.setClientCertificate(request.getClientCertificate());
        entity.setTlsVerify(request.getTlsVerify());
        entity.setTlsVerifyDepth(request.getTlsVerifyDepth());
        entity.setCaCertificates(request.getCaCertificates());
        kongServiceRepository.saveAndFlush(entity);

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", kongId);
        payload.put("name", fullName);
        if (request.getHost() != null) payload.put("host", request.getHost());
        if (request.getPort() != null) payload.put("port", request.getPort());
        if (request.getProtocol() != null) payload.put("protocol", request.getProtocol());
        if (StringUtils.hasText(request.getPath())) payload.put("path", request.getPath().trim());
        if (request.getEnabled() != null) payload.put("enabled", request.getEnabled());
        if (request.getTags() != null) payload.put("tags", request.getTags());
        if (request.getRetries() != null) payload.put("retries", request.getRetries());
        if (request.getConnectTimeout() != null) payload.put("connect_timeout", request.getConnectTimeout());
        if (request.getWriteTimeout() != null) payload.put("write_timeout", request.getWriteTimeout());
        if (request.getReadTimeout() != null) payload.put("read_timeout", request.getReadTimeout());
        if (request.getProtocol() != null && (request.getProtocol().equalsIgnoreCase("https") || request.getProtocol().equalsIgnoreCase("tls") || request.getProtocol().equalsIgnoreCase("grpcs"))) {
            if (StringUtils.hasText(request.getClientCertificate())) payload.put("client_certificate", Map.of("id", request.getClientCertificate()));
            if (request.getTlsVerify() != null) payload.put("tls_verify", request.getTlsVerify());
            if (request.getTlsVerifyDepth() != null) payload.put("tls_verify_depth", request.getTlsVerifyDepth());
            if (request.getCaCertificates() != null) payload.put("ca_certificates", request.getCaCertificates());
        }
        kongAdminClient.createService(kongInstanceId, projectCode, payload);

        // 3. Auto-enable plugins on this service
        kongPluginService.enableHttpLogPlugin(kongInstanceId, projectCode, "services", kongId);
        
        try {
            if (Boolean.TRUE.equals(project.getJwtAuthEnabled())) {
                // Enable JWT plugin to verify signatures
                kongJwtProvisioningService.enableJwtPlugin(kongInstanceId, projectCode, "services", kongId);
                
                // Enable ACL plugin to restrict to this project's IdP Client
                String aclGroup = kongJwtProvisioningService.getAclGroupForProject(projectCode);
                kongJwtProvisioningService.enableAclPlugin(kongInstanceId, projectCode, "services", kongId, aclGroup);
            }
        } catch (Exception e) {
            log.error("Failed to auto-enable auth plugins for service {}: {}", kongId, e.getMessage());
        }

        return kongMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public KongServiceDto getService(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongServiceEntity entity = kongServiceRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));
        return kongMapper.toDto(entity);
    }

    @Transactional
    public KongServiceDto updateService(Long kongInstanceId, String projectCode, Long id, KongServiceRequest request) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongServiceEntity entity = kongServiceRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));

        String fullName = namingEnforcer.formatName(projectCode, request.getName());

        // 1. Update DB first (flush to validate constraints immediately)
        entity.setName(fullName);
        entity.setDisplayName(request.getName());
        entity.setHost(request.getHost());
        entity.setPort(request.getPort());
        entity.setProtocol(request.getProtocol());
        entity.setPath(StringUtils.hasText(request.getPath()) ? request.getPath().trim() : null);
        entity.setEnabled(request.getEnabled());
        entity.setSyncStatus(SyncStatus.SYNCED);
        entity.setTags(kongMapper.listToJson(request.getTags()));
        entity.setRetries(request.getRetries());
        entity.setConnectTimeout(request.getConnectTimeout());
        entity.setWriteTimeout(request.getWriteTimeout());
        entity.setReadTimeout(request.getReadTimeout());
        entity.setClientCertificate(request.getClientCertificate());
        entity.setTlsVerify(request.getTlsVerify());
        entity.setTlsVerifyDepth(request.getTlsVerifyDepth());
        entity.setCaCertificates(request.getCaCertificates());
        kongServiceRepository.saveAndFlush(entity);

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", fullName);
        if (request.getHost() != null) payload.put("host", request.getHost());
        if (request.getPort() != null) payload.put("port", request.getPort());
        if (request.getProtocol() != null) payload.put("protocol", request.getProtocol());
        if (StringUtils.hasText(request.getPath())) payload.put("path", request.getPath().trim());
        if (request.getEnabled() != null) payload.put("enabled", request.getEnabled());
        if (request.getTags() != null) payload.put("tags", request.getTags());
        if (request.getRetries() != null) payload.put("retries", request.getRetries());
        if (request.getConnectTimeout() != null) payload.put("connect_timeout", request.getConnectTimeout());
        if (request.getWriteTimeout() != null) payload.put("write_timeout", request.getWriteTimeout());
        if (request.getReadTimeout() != null) payload.put("read_timeout", request.getReadTimeout());
        if (request.getProtocol() != null && (request.getProtocol().equalsIgnoreCase("https") || request.getProtocol().equalsIgnoreCase("tls") || request.getProtocol().equalsIgnoreCase("grpcs"))) {
            if (StringUtils.hasText(request.getClientCertificate())) payload.put("client_certificate", Map.of("id", request.getClientCertificate()));
            if (request.getTlsVerify() != null) payload.put("tls_verify", request.getTlsVerify());
            if (request.getTlsVerifyDepth() != null) payload.put("tls_verify_depth", request.getTlsVerifyDepth());
            if (request.getCaCertificates() != null) payload.put("ca_certificates", request.getCaCertificates());
        }
        kongAdminClient.updateService(kongInstanceId, projectCode, entity.getKongId(), payload);

        return kongMapper.toDto(entity);
    }

    @Transactional
    public void deleteService(Long kongInstanceId, String projectCode, Long id) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
        KongServiceEntity entity = kongServiceRepository.findByIdAndProjectIdAndKongInstanceId(id, project.getId(), kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Service not found: " + id));

        String kongIdToDelete = entity.getKongId();

        // 1. Delete from DB first (flush to execute SQL immediately)
        kongServiceRepository.delete(entity);
        kongServiceRepository.flush();

        // 2. Call Kong API (if this fails, @Transactional will rollback DB)
        kongAdminClient.deleteService(kongInstanceId, projectCode, kongIdToDelete);
    }
}
