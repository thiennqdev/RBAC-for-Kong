package com.vcc.template.storage.service;

import com.vcc.template.project.entity.Project;
import com.vcc.template.project.repository.ProjectRepository;
import com.vcc.template.storage.dto.FileItemDto;
import com.vcc.template.storage.dto.StorageUsageDto;
import com.vcc.template.storage.dto.UploadUrlDto;
import com.vcc.template.storage.entity.ProjectStorageConfig;
import com.vcc.template.storage.repository.ProjectStorageConfigRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileExplorerService {

    @Autowired(required = false)
    private MinioClient minioClient;

    private final ProjectRepository projectRepository;
    private final ProjectStorageConfigRepository storageConfigRepository;
    private final StorageProvisioningService storageProvisioningService;

    private ProjectStorageConfig getConfig(String projectCode) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectCode));
        return storageConfigRepository.findByProject_ProjectCode(projectCode)
                .orElseThrow(() -> new IllegalArgumentException("Storage not enabled for project: " + projectCode));
    }

    public List<FileItemDto> listFiles(String projectCode, String prefix) {
        if (minioClient == null) throw new IllegalStateException("MinIO is not configured.");
        String bucketName = getConfig(projectCode).getBucketName();

        if (prefix != null && !prefix.isBlank() && !prefix.endsWith("/")) {
            prefix += "/";
        }

        List<FileItemDto> items = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix != null && !prefix.isBlank() ? prefix : null)
                            .recursive(false)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                
                String rawName = item.objectName();
                if (rawName == null) {
                    continue; // Skip if null
                }
                
                // Skip the exact folder object itself
                if (prefix != null && rawName.equals(prefix)) {
                    continue;
                }
                
                FileItemDto dto = FileItemDto.builder()
                        .name(extractName(rawName))
                        .path(rawName)
                        .isDir(item.isDir())
                        .size(item.isDir() ? 0 : item.size())
                        .lastModified(item.isDir() ? null : (item.lastModified() != null ? item.lastModified().toInstant() : null))
                        .build();
                items.add(dto);
            }
        } catch (Exception e) {
            log.error("Failed to list files for project {}", projectCode, e);
            throw new RuntimeException("Failed to list files: " + e.getMessage(), e);
        }
        return items;
    }

    public List<String> listAllFolders(String projectCode) {
        if (minioClient == null) throw new IllegalStateException("MinIO is not configured.");
        String bucketName = getConfig(projectCode).getBucketName();
        java.util.Set<String> folderSet = new java.util.HashSet<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                String rawName = item.objectName();
                if (rawName == null) continue;
                
                // If it's explicitly an empty directory object
                if (rawName.endsWith("/")) {
                    folderSet.add(rawName);
                }
                
                // Extract all parent paths
                String[] parts = rawName.split("/");
                StringBuilder currentPath = new StringBuilder();
                // If it ends with /, the last part is empty, length is parts.length
                // If it's a file, the last part is the file name, so loop until length - 1
                int limit = rawName.endsWith("/") ? parts.length : parts.length - 1;
                for (int i = 0; i < limit; i++) {
                    if (parts[i].isEmpty()) continue;
                    currentPath.append(parts[i]).append("/");
                    folderSet.add(currentPath.toString());
                }
            }
        } catch (Exception e) {
            log.error("Failed to list all folders for project {}", projectCode, e);
            throw new RuntimeException("Failed to list folders: " + e.getMessage(), e);
        }
        
        List<String> sortedFolders = new ArrayList<>(folderSet);
        java.util.Collections.sort(sortedFolders);
        return sortedFolders;
    }

    public UploadUrlDto getUploadPresignedUrl(String projectCode, String filePath) {
        if (minioClient == null) throw new IllegalStateException("MinIO is not configured.");
        String bucketName = getConfig(projectCode).getBucketName();
        
        String finalFilePath = filePath;
        int copyIndex = 1;
        while (true) {
            try {
                minioClient.statObject(
                        io.minio.StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(finalFilePath)
                                .build()
                );
                // Object exists, we need to rename
                int dotIndex = filePath.lastIndexOf('.');
                if (dotIndex > 0) {
                    finalFilePath = filePath.substring(0, dotIndex) + "(" + copyIndex + ")" + filePath.substring(dotIndex);
                } else {
                    finalFilePath = filePath + "(" + copyIndex + ")";
                }
                copyIndex++;
            } catch (io.minio.errors.ErrorResponseException e) {
                if ("NoSuchKey".equals(e.errorResponse().code())) {
                    // Object does not exist, we found a free name
                    break;
                }
                throw new RuntimeException("Failed to check if file exists: " + e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException("Failed to check if file exists: " + e.getMessage());
            }
        }

        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(finalFilePath)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
            return UploadUrlDto.builder()
                    .url(url)
                    .finalFilePath(finalFilePath)
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate upload URL for project {}", projectCode, e);
            throw new RuntimeException("Failed to generate upload URL: " + e.getMessage());
        }
    }

    public String getDownloadPresignedUrl(String projectCode, String filePath, Integer expiryHours, boolean inline) {
        if (minioClient == null) throw new IllegalStateException("MinIO is not configured.");
        String bucketName = getConfig(projectCode).getBucketName();
        int expiry = (expiryHours != null && expiryHours > 0) ? expiryHours : 1;
        String fileName = extractName(filePath);
        try {
            java.util.Map<String, String> queryParams = new java.util.HashMap<>();
            if (inline) {
                queryParams.put("response-content-disposition", "inline");
            } else {
                queryParams.put("response-content-disposition", "attachment; filename=\"" + fileName + "\"");
            }
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(filePath)
                            .expiry(expiry, TimeUnit.HOURS)
                            .extraQueryParams(queryParams)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate download URL for project {}", projectCode, e);
            throw new RuntimeException("Failed to generate download URL: " + e.getMessage());
        }
    }

    public void deleteFile(String projectCode, String filePath) {
        if (minioClient == null) throw new IllegalStateException("MinIO is not configured.");
        String bucketName = getConfig(projectCode).getBucketName();
        try {
            if (filePath.endsWith("/")) {
                Iterable<Result<Item>> objects = minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucketName).prefix(filePath).recursive(true).build()
                );
                for (Result<Item> result : objects) {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(result.get().objectName())
                                    .build()
                    );
                }
            } else {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(filePath)
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Failed to delete file for project {}", projectCode, e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }

    public void bulkDeleteFiles(String projectCode, List<String> filePaths) {
        if (minioClient == null) throw new IllegalStateException("MinIO is not configured.");
        String bucketName = getConfig(projectCode).getBucketName();
        
        for (String filePath : filePaths) {
            try {
                if (filePath.endsWith("/")) {
                    Iterable<Result<Item>> objects = minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(bucketName).prefix(filePath).recursive(true).build()
                    );
                    for (Result<Item> result : objects) {
                        minioClient.removeObject(
                                RemoveObjectArgs.builder().bucket(bucketName).object(result.get().objectName()).build()
                        );
                    }
                } else {
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(filePath)
                                    .build()
                    );
                }
            } catch (Exception e) {
                log.warn("Failed to delete file {} during bulk delete: {}", filePath, e.getMessage());
            }
        }
    }

    public void createFolder(String projectCode, String folderPath) {
        if (minioClient == null) throw new IllegalStateException("MinIO is not configured.");
        String bucketName = getConfig(projectCode).getBucketName();
        
        if (!folderPath.endsWith("/")) {
            folderPath += "/";
        }
        
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(folderPath)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to create folder for project {}", projectCode, e);
            throw new RuntimeException("Failed to create folder: " + e.getMessage());
        }
    }

    public void setFolderPermission(String projectCode, String folderPath, boolean isPublic) {
        String bucketName = getConfig(projectCode).getBucketName();
        if (folderPath.endsWith("/")) {
            folderPath = folderPath.substring(0, folderPath.length() - 1);
        }
        try {
            storageProvisioningService.updateFolderPolicy(bucketName, folderPath, isPublic);
        } catch (Exception e) {
            log.error("Failed to set folder permission for project {}", projectCode, e);
            throw new RuntimeException("Failed to set folder permission: " + e.getMessage());
        }
    }

    public StorageUsageDto getStorageUsage(String projectCode) {
        if (minioClient == null) throw new IllegalStateException("MinIO is not configured.");
        ProjectStorageConfig config = getConfig(projectCode);
        String bucketName = config.getBucketName();
        
        long usedBytes = 0;
        int objectCount = 0;
        
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );
            
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    usedBytes += item.size();
                    objectCount++;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to calculate storage usage for project {}: {}", projectCode, e.getMessage());
        }
        
        return StorageUsageDto.builder()
                .usedBytes(usedBytes)
                .quotaBytes(config.getQuotaInBytes())
                .objectCount(objectCount)
                .build();
    }

    private String extractName(String objectName) {
        if (objectName.endsWith("/")) {
            objectName = objectName.substring(0, objectName.length() - 1);
        }
        int lastSlash = objectName.lastIndexOf('/');
        return lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;
    }
}
