package com.vcc.template.kong.controller;


import com.vcc.template.kong.dto.KongRouteDto;
import com.vcc.template.kong.dto.KongRouteRequest;
import com.vcc.template.kong.service.KongRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectCode}/kong")
@RequiredArgsConstructor
public class KongRouteController {

    private final KongRouteService kongRouteService;

    @GetMapping("/services/{serviceId}/routes")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<List<KongRouteDto>> getRoutes(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                     @PathVariable Long serviceId) {
        return ResponseEntity.ok(kongRouteService.getRoutes(kongInstanceId, projectCode, serviceId));
    }

    @PostMapping("/services/{serviceId}/routes")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongRouteDto> createRoute(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                 @PathVariable Long serviceId,
                                                                 @Valid @RequestBody KongRouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kongRouteService.createRoute(kongInstanceId, projectCode, serviceId, request));
    }

    @GetMapping("/routes/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<KongRouteDto> getRoute(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                              @PathVariable Long id) {
        return ResponseEntity.ok(kongRouteService.getRoute(kongInstanceId, projectCode, id));
    }

    @PutMapping("/routes/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongRouteDto> updateRoute(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                 @PathVariable Long id,
                                                                 @Valid @RequestBody KongRouteRequest request) {
        return ResponseEntity.ok(kongRouteService.updateRoute(kongInstanceId, projectCode, id, request));
    }

    @DeleteMapping("/routes/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<Void> deleteRoute(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                         @PathVariable Long id) {
        kongRouteService.deleteRoute(kongInstanceId, projectCode, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/routes/{id}/request-approval")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongRouteDto> requestApproval(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(kongRouteService.requestApproval(kongInstanceId, projectCode, id));
    }

    @PostMapping("/routes/{id}/approve")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN')")
    public ResponseEntity<KongRouteDto> approveRoute(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                     @PathVariable Long id) {
        return ResponseEntity.ok(kongRouteService.approveRoute(kongInstanceId, projectCode, id));
    }

    @PostMapping("/routes/{id}/reject")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN')")
    public ResponseEntity<KongRouteDto> rejectRoute(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                    @PathVariable Long id) {
        return ResponseEntity.ok(kongRouteService.rejectRoute(kongInstanceId, projectCode, id));
    }
}
