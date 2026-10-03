package com.vcc.template.kong.controller;


import com.vcc.template.kong.dto.KongPluginDto;
import com.vcc.template.kong.dto.KongPluginRequest;
import com.vcc.template.kong.service.KongPluginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectCode}/kong/plugins")
@RequiredArgsConstructor
public class KongPluginController {

    private final KongPluginService kongPluginService;

    @GetMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<List<KongPluginDto>> getPlugins(@RequestParam Long kongInstanceId, @PathVariable String projectCode) {
        return ResponseEntity.ok(kongPluginService.getPlugins(kongInstanceId, projectCode));
    }

    @PostMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongPluginDto> createPlugin(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                   @Valid @RequestBody KongPluginRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kongPluginService.createPlugin(kongInstanceId, projectCode, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<KongPluginDto> getPlugin(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                @PathVariable Long id) {
        return ResponseEntity.ok(kongPluginService.getPlugin(kongInstanceId, projectCode, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongPluginDto> updatePlugin(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                   @PathVariable Long id,
                                                                   @Valid @RequestBody KongPluginRequest request) {
        return ResponseEntity.ok(kongPluginService.updatePlugin(kongInstanceId, projectCode, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<Void> deletePlugin(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                          @PathVariable Long id) {
        kongPluginService.deletePlugin(kongInstanceId, projectCode, id);
        return ResponseEntity.noContent().build();
    }
}
