package com.vcc.template.kong.service;

import com.vcc.template.kong.dto.KongInstanceDto;
import com.vcc.template.kong.dto.KongInstanceCreateRequest;
import com.vcc.template.kong.entity.KongInstanceEntity;
import com.vcc.template.kong.repository.KongInstanceRepository;
import com.vcc.template.kong.client.KongClientFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KongInstanceService {

    private final KongInstanceRepository kongInstanceRepository;
    private final KongClientFactory kongClientFactory;

    public List<KongInstanceDto> getAllInstances() {
        return kongInstanceRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public KongInstanceDto getInstance(Long id) {
        return kongInstanceRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new IllegalArgumentException("Kong Instance not found with id: " + id));
    }

    public KongInstanceDto createInstance(KongInstanceCreateRequest request) {
        if (kongInstanceRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Kong Instance with code '" + request.getCode() + "' already exists");
        }

        KongInstanceEntity entity = new KongInstanceEntity();
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setAdminUrl(request.getAdminUrl());
        entity.setDescription(request.getDescription());
        entity.setStatus("ACTIVE");

        KongInstanceEntity saved = kongInstanceRepository.save(entity);
        return mapToDto(saved);
    }

    public void deleteInstance(Long id) {
        if (!kongInstanceRepository.existsById(id)) {
            throw new IllegalArgumentException("Kong Instance with id '" + id + "' not found");
        }
        kongInstanceRepository.deleteById(id);
        kongClientFactory.evictClient(id);
    }

    public KongInstanceDto updateInstance(Long id, com.vcc.template.kong.dto.KongInstanceUpdateRequest request) {
        KongInstanceEntity entity = kongInstanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kong Instance with id '" + id + "' not found"));

        entity.setName(request.getName());
        entity.setAdminUrl(request.getAdminUrl());
        entity.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        KongInstanceEntity saved = kongInstanceRepository.save(entity);
        kongClientFactory.evictClient(id);
        return mapToDto(saved);
    }

    private KongInstanceDto mapToDto(KongInstanceEntity entity) {
        KongInstanceDto dto = new KongInstanceDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setAdminUrl(entity.getAdminUrl());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
