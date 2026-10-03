package com.vcc.template.storage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage.minio")
@Getter
@Setter
public class StorageProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String region = "us-east-1";
}
