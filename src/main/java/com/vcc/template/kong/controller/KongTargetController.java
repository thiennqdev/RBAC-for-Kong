package com.vcc.template.kong.controller;

import com.vcc.template.kong.dto.KongTargetDto;
import com.vcc.template.kong.dto.KongTargetRequest;
import com.vcc.template.kong.service.KongTargetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectCode}/kong/upstreams/{upstreamId}/targets")
@RequiredArgsConstructor
public class KongTargetController {

    private final KongTargetService kongTargetService;

    @GetMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<List<KongTargetDto>> getTargets(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                          @PathVariable Long upstreamId) {
        return ResponseEntity.ok(kongTargetService.getTargets(kongInstanceId, projectCode, upstreamId));
    }

    @PostMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongTargetDto> addTarget(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                   @PathVariable Long upstreamId,
                                                   @Valid @RequestBody KongTargetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kongTargetService.addTarget(kongInstanceId, projectCode, upstreamId, request));
    }

    @DeleteMapping("/{targetId}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<Void> deleteTarget(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                             @PathVariable Long upstreamId,
                                             @PathVariable Long targetId) {
        kongTargetService.deleteTarget(kongInstanceId, projectCode, upstreamId, targetId);
        return ResponseEntity.noContent().build();
    }
}
