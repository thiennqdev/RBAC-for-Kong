package com.vcc.template.kong.controller;


import com.vcc.template.kong.dto.KongServiceDto;
import com.vcc.template.kong.dto.KongServiceRequest;
import com.vcc.template.kong.service.KongServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectCode}/kong/services")
@RequiredArgsConstructor
public class KongServiceController {

    private final KongServiceService kongServiceService;

    @GetMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<List<KongServiceDto>> getServices(@RequestParam Long kongInstanceId, @PathVariable String projectCode) {
        return ResponseEntity.ok(kongServiceService.getServices(kongInstanceId, projectCode));
    }

    @PostMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongServiceDto> createService(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                     @Valid @RequestBody KongServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kongServiceService.createService(kongInstanceId, projectCode, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<KongServiceDto> getService(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                  @PathVariable Long id) {
        return ResponseEntity.ok(kongServiceService.getService(kongInstanceId, projectCode, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<KongServiceDto> updateService(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                                     @PathVariable Long id,
                                                                     @Valid @RequestBody KongServiceRequest request) {
        return ResponseEntity.ok(kongServiceService.updateService(kongInstanceId, projectCode, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<Void> deleteService(@RequestParam Long kongInstanceId, @PathVariable String projectCode,
                                                           @PathVariable Long id) {
        kongServiceService.deleteService(kongInstanceId, projectCode, id);
        return ResponseEntity.noContent().build();
    }
}
