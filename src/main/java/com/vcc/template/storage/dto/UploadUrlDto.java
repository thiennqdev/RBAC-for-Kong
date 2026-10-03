package com.vcc.template.storage.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadUrlDto {
    private String url;
    private String finalFilePath;
}
