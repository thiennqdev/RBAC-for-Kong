package com.vcc.template.common.file.controller;

import com.vcc.template.common.file.dto.FileUploadResponse;
import com.vcc.template.common.file.errorcode.FileErrorCode;
import com.vcc.template.common.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

/**
 * Controller for file upload operations.
 * Provides endpoints for uploading files to object storage.
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Upload", description = "File upload and management operations")
class FileUploadController {

    private final FileStorageService fileStorageService;

    // Category-specific validation rules
    private static final Map<String, CategoryConfig> CATEGORY_CONFIGS = Map.of(
            "project-icons", new CategoryConfig(
                    Set.of("image/png", "image/jpeg", "image/svg+xml"),
                    5 * 1024 * 1024 // 5MB
            ),
            "attachments", new CategoryConfig(
                    Set.of("application/pdf", "image/png", "image/jpeg", "application/msword"),
                    10 * 1024 * 1024 // 10MB
            )
    );

    @PostMapping(path = "/upload", version = "1.0", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Upload a file",
            description = "Upload a file to object storage under the specified category. " +
                    "Validates file type and size based on category rules."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid file or category"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SYSTEM_ADMIN role"),
            @ApiResponse(responseCode = "500", description = "File upload failed")
    })
    ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") @NotBlank String category
    ) {
        log.info("File upload request received: category={}, filename={}, size={}",
                category, file.getOriginalFilename(), file.getSize());

        // Validate category
        CategoryConfig config = CATEGORY_CONFIGS.get(category);
        if (config == null) {
            throw FileErrorCode.INVALID_CATEGORY.toException(category);
        }

        // Validate file
        fileStorageService.validateFile(file, config.allowedMimeTypes(), config.maxSizeBytes());

        // Upload file
        FileUploadResponse response = fileStorageService.uploadFile(file, category);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Configuration for file upload validation per category.
     */
    private record CategoryConfig(
            Set<String> allowedMimeTypes,
            long maxSizeBytes
    ) {
    }
}
