package com.vcc.template.common.file.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for file upload operations.
 *
 * @param filePath the path to the uploaded file in storage (e.g., "project-icons/uuid.png")
 * @param fileName the original filename
 * @param contentType the MIME type of the file
 * @param fileSize the size of the file in bytes
 * @param uploadedAt the timestamp when the file was uploaded
 */
public record FileUploadResponse(
        String filePath,
        String fileName,
        String contentType,
        long fileSize,
        LocalDateTime uploadedAt
) {}
