package com.vcc.template.common.file.service.impl;

import com.vcc.template.common.config.S3Config;
import com.vcc.template.common.file.dto.FileUploadResponse;
import com.vcc.template.common.file.errorcode.FileErrorCode;
import com.vcc.template.common.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * AWS S3 SDK implementation of FileStorageService.
 * Compatible with both AWS S3 and MinIO (S3-compatible storage).
 */
@Service
@RequiredArgsConstructor
@Slf4j
class S3FileStorageServiceImpl implements FileStorageService {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;
    
    @Override
    public FileUploadResponse uploadFile(MultipartFile file, String category) {
        try {
            ensureBucketExists();
            
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID() + extension;
            String filePath = category + "/" + fileName;
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucket())
                    .key(filePath)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("File uploaded successfully: {}", filePath);
            
            return new FileUploadResponse(
                    filePath,
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw FileErrorCode.FILE_UPLOAD_FAILED.toException(file.getOriginalFilename());
        }
    }
    
    @Override
    public String getFileUrl(String filePath) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Config.getBucket())
                    .key(filePath)
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for file: {}", filePath, e);
            throw FileErrorCode.FILE_UPLOAD_FAILED.toException(filePath);
        }
    }
    
    @Override
    public void deleteFile(String filePath) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Config.getBucket())
                    .key(filePath)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", filePath, e);
            throw FileErrorCode.FILE_UPLOAD_FAILED.toException(filePath);
        }
    }
    
    @Override
    public boolean fileExists(String filePath) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Config.getBucket())
                    .key(filePath)
                    .build();
            
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence: {}", filePath, e);
            return false;
        }
    }
    
    @Override
    public void validateFile(MultipartFile file, Set<String> allowedMimeTypes, long maxSizeBytes) {
        if (file == null || file.isEmpty()) {
            throw FileErrorCode.INVALID_FILE_TYPE.toException(
                    file != null ? file.getContentType() : "null", 
                    String.join(", ", allowedMimeTypes)
            );
        }
        
        if (!allowedMimeTypes.contains(file.getContentType())) {
            throw FileErrorCode.INVALID_FILE_TYPE.toException(
                    file.getContentType(), 
                    String.join(", ", allowedMimeTypes)
            );
        }
        
        if (file.getSize() > maxSizeBytes) {
            throw FileErrorCode.FILE_TOO_LARGE.toException(
                    formatFileSize(file.getSize()), 
                    formatFileSize(maxSizeBytes)
            );
        }
    }
    
    private void ensureBucketExists() {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(s3Config.getBucket())
                    .build();
            
            s3Client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            try {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(s3Config.getBucket())
                        .build();
                
                s3Client.createBucket(createBucketRequest);
                log.info("Bucket created: {}", s3Config.getBucket());
            } catch (Exception ex) {
                log.error("Failed to create bucket: {}", s3Config.getBucket(), ex);
                throw FileErrorCode.FILE_UPLOAD_FAILED.toException(s3Config.getBucket());
            }
        } catch (Exception e) {
            log.error("Failed to check bucket existence: {}", s3Config.getBucket(), e);
            throw FileErrorCode.FILE_UPLOAD_FAILED.toException(s3Config.getBucket());
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
