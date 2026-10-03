package com.vcc.template.common.file.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for file upload operations.
 *
 * @param category the category/folder for organizing files (e.g., "project-icons", "attachments")
 * @param file the file to upload
 */
public record FileUploadRequest(
        @NotBlank(message = "{FILE-004}")
        String category,
        
        MultipartFile file
) {}
