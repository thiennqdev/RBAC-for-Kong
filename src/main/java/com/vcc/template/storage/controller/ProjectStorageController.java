package com.vcc.template.storage.controller;

import com.vcc.template.storage.dto.EnableStorageRequest;
import com.vcc.template.storage.dto.StorageConfigResponse;
import com.vcc.template.storage.service.ProjectStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects/{projectCode}/storage")
@RequiredArgsConstructor
@Tag(name = "Project Storage", description = "File Management APIs")
public class ProjectStorageController {

    private final ProjectStorageService projectStorageService;

    @PostMapping("/enable")
    @PreAuthorize("@projectSecurity.hasRole(#projectCode, 'PROJECT_ADMIN')")
    @Operation(summary = "Enable file storage for a project")
    public ResponseEntity<StorageConfigResponse> enableStorage(
            @PathVariable String projectCode,
            @Valid @RequestBody EnableStorageRequest request) {
        
        StorageConfigResponse response = projectStorageService.enableStorageForProject(projectCode, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    @Operation(summary = "Get storage configuration for a project")
    public ResponseEntity<StorageConfigResponse> getStorageConfig(
            @PathVariable String projectCode) {
        
        StorageConfigResponse response = projectStorageService.getStorageConfig(projectCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/credentials/rotate")
    @PreAuthorize("@projectSecurity.hasRole(#projectCode, 'PROJECT_ADMIN')")
    @Operation(summary = "Rotate storage credentials (revoke old, generate new)")
    public ResponseEntity<StorageConfigResponse> rotateCredentials(
            @PathVariable String projectCode) {
        
        StorageConfigResponse response = projectStorageService.rotateCredentials(projectCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/policies")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    @Operation(summary = "Get advanced storage policies")
    public ResponseEntity<java.util.List<com.vcc.template.storage.dto.StoragePolicyRuleDto>> getPolicies(
            @PathVariable String projectCode) {
        // ProjectStorageService delegates to StorageProvisioningService
        return ResponseEntity.ok(projectStorageService.getCustomPolicies(projectCode));
    }

    @PutMapping("/policies")
    @PreAuthorize("@projectSecurity.hasRole(#projectCode, 'PROJECT_ADMIN')")
    @Operation(summary = "Update advanced storage policies")
    public ResponseEntity<Void> updatePolicies(
            @PathVariable String projectCode,
            @RequestBody com.vcc.template.storage.dto.StoragePolicyUpdateRequest request) {
        projectStorageService.setCustomPolicies(projectCode, request.getRules());
        return ResponseEntity.ok().build();
    }
}
