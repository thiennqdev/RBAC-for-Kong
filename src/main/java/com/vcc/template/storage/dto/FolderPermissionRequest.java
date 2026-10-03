package com.vcc.template.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FolderPermissionRequest {
    @NotBlank(message = "Folder path cannot be blank")
    private String folderPath;

    @NotNull(message = "isPublic flag cannot be null")
    private Boolean isPublic;
}
