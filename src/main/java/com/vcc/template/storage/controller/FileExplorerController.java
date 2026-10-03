package com.vcc.template.storage.controller;

import com.vcc.template.storage.dto.FileItemDto;
import com.vcc.template.storage.dto.PresignedUrlResponse;
import com.vcc.template.storage.dto.StorageUsageDto;
import com.vcc.template.storage.dto.UploadUrlDto;
import com.vcc.template.storage.service.FileExplorerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectCode}/storage/explorer")
@RequiredArgsConstructor
public class FileExplorerController {

    private final FileExplorerService fileExplorerService;

    @GetMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<List<FileItemDto>> listFiles(
            @PathVariable String projectCode,
            @RequestParam(required = false) String prefix) {
        List<FileItemDto> files = fileExplorerService.listFiles(projectCode, prefix);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/folders")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<List<String>> listAllFolders(
            @PathVariable String projectCode) {
        List<String> folders = fileExplorerService.listAllFolders(projectCode);
        return ResponseEntity.ok(folders);
    }

    @GetMapping("/upload-url")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<UploadUrlDto> getUploadUrl(
            @PathVariable String projectCode,
            @RequestParam String filePath) {
        UploadUrlDto dto = fileExplorerService.getUploadPresignedUrl(projectCode, filePath);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/download-url")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<PresignedUrlResponse> getDownloadUrl(
            @PathVariable String projectCode,
            @RequestParam String filePath,
            @RequestParam(required = false, defaultValue = "false") boolean inline,
            @RequestParam(required = false) Integer expiryHours) {
        String url = fileExplorerService.getDownloadPresignedUrl(projectCode, filePath, expiryHours, inline);
        return ResponseEntity.ok(new PresignedUrlResponse(url, "GET"));
    }

    @DeleteMapping
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<Void> deleteFile(
            @PathVariable String projectCode,
            @RequestParam String filePath) {
        fileExplorerService.deleteFile(projectCode, filePath);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-delete")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<Void> bulkDeleteFiles(
            @PathVariable String projectCode,
            @RequestBody List<String> filePaths) {
        fileExplorerService.bulkDeleteFiles(projectCode, filePaths);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/folder")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<Void> createFolder(
            @PathVariable String projectCode,
            @RequestParam String folderPath) {
        fileExplorerService.createFolder(projectCode, folderPath);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/folder/permission")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER')")
    public ResponseEntity<Void> setFolderPermission(
            @PathVariable String projectCode,
            @jakarta.validation.Valid @RequestBody com.vcc.template.storage.dto.FolderPermissionRequest request) {
        fileExplorerService.setFolderPermission(projectCode, request.getFolderPath(), request.getIsPublic());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/usage")
    @PreAuthorize("@projectSecurity.hasAnyRole(#projectCode, 'PROJECT_ADMIN', 'MAINTAINER', 'DEVELOPER')")
    public ResponseEntity<StorageUsageDto> getUsage(
            @PathVariable String projectCode) {
        StorageUsageDto usage = fileExplorerService.getStorageUsage(projectCode);
        return ResponseEntity.ok(usage);
    }
}
