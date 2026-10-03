package com.vcc.template.kong.controller;


import com.vcc.template.kong.dto.KongUpstreamDto;
import com.vcc.template.kong.dto.KongUpstreamRequest;
import com.vcc.template.kong.service.KongUpstreamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectCode}/kong/upstreams")
@RequiredArgsConstructor
public class KongUpstreamController {

    private final KongUpstreamService kongUpstreamService;

    @GetMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<List<KongUpstreamDto>> getUpstreams(@RequestParam Long kongInstanceId, @PathVariable String projectCode) {
        return ResponseEntity.ok(kongUpstreamService.getUpstreams(kongInstanceId, projectCode));
    }

    @PostMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongUpstreamDto> createUpstream(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                       @Valid @RequestBody KongUpstreamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kongUpstreamService.createUpstream(kongInstanceId, projectCode, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<KongUpstreamDto> getUpstream(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                    @PathVariable Long id) {
        return ResponseEntity.ok(kongUpstreamService.getUpstream(kongInstanceId, projectCode, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongUpstreamDto> updateUpstream(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                       @PathVariable Long id,
                                                                       @Valid @RequestBody KongUpstreamRequest request) {
        return ResponseEntity.ok(kongUpstreamService.updateUpstream(kongInstanceId, projectCode, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<Void> deleteUpstream(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                            @PathVariable Long id) {
        kongUpstreamService.deleteUpstream(kongInstanceId, projectCode, id);
        return ResponseEntity.noContent().build();
    }
}
