package com.vcc.template.kong.controller;

import com.vcc.template.kong.dto.KongInstanceDto;
import com.vcc.template.kong.service.KongInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.vcc.template.kong.dto.KongInstanceCreateRequest;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/kong-instances")
@RequiredArgsConstructor
public class KongInstanceController {

    private final KongInstanceService kongInstanceService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KongInstanceDto>> getInstances() {
        return ResponseEntity.ok(kongInstanceService.getAllInstances());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KongInstanceDto> getInstance(@PathVariable Long id) {
        return ResponseEntity.ok(kongInstanceService.getInstance(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<KongInstanceDto> createInstance(@Valid @RequestBody KongInstanceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(kongInstanceService.createInstance(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteInstance(@PathVariable Long id) {
        kongInstanceService.deleteInstance(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<KongInstanceDto> updateInstance(@PathVariable Long id, @Valid @RequestBody com.vcc.template.kong.dto.KongInstanceUpdateRequest request) {
        return ResponseEntity.ok(kongInstanceService.updateInstance(id, request));
    }
}
