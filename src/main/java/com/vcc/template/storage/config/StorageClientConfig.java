package com.vcc.template.storage.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StorageClientConfig {

    private final StorageProperties storageProperties;

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.storage.minio.endpoint")
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(storageProperties.getEndpoint())
                .credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey())
                .build();
    }
}
