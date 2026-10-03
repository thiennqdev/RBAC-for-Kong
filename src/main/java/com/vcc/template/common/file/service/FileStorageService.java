package com.vcc.template.common.file.service;

import com.vcc.template.common.file.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Interface for file storage operations.
 * Provides abstraction for storing, retrieving, and managing files in object storage.
 */
public interface FileStorageService {
    
    /**
     * Upload a file to storage under the specified category.
     *
     * @param file the file to upload
     * @param category the category/folder for organizing files (e.g., "project-icons", "attachments")
     * @return FileUploadResponse containing file path and metadata
     */
    FileUploadResponse uploadFile(MultipartFile file, String category);
    
    /**
     * Get a presigned or public URL for accessing a file.
     *
     * @param filePath the path to the file in storage
     * @return URL string for accessing the file
     */
    String getFileUrl(String filePath);
    
    /**
     * Delete a file from storage.
     *
     * @param filePath the path to the file to delete
     */
    void deleteFile(String filePath);
    
    /**
     * Check if a file exists in storage.
     *
     * @param filePath the path to check
     * @return true if file exists, false otherwise
     */
    boolean fileExists(String filePath);
    
    /**
     * Validate a file against allowed MIME types and size constraints.
     *
     * @param file the file to validate
     * @param allowedMimeTypes set of allowed MIME types
     * @param maxSizeBytes maximum allowed file size in bytes
     * @throws com.vcc.template.common.exception.BusinessException if validation fails
     */
    void validateFile(MultipartFile file, Set<String> allowedMimeTypes, long maxSizeBytes);
}
