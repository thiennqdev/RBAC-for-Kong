package com.vcc.template.kong.client;

import com.vcc.template.kong.config.KongAdminProperties;
import com.vcc.template.kong.entity.KongInstanceEntity;
import com.vcc.template.kong.repository.KongInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class KongClientFactory {

    private final KongInstanceRepository kongInstanceRepository;
    private final KongAdminProperties properties;
    private final RestClient.Builder restClientBuilder;
    
    private final Map<Long, RestClient> clients = new ConcurrentHashMap<>();

    public RestClient getClient(Long kongInstanceId) {
        if (kongInstanceId == null) {
            throw new IllegalArgumentException("Kong Instance ID is required");
        }

        return clients.computeIfAbsent(kongInstanceId, id -> {
            log.info("Initializing RestClient for Kong Instance ID: {}", id);
            KongInstanceEntity instance = kongInstanceRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Kong Instance not found: " + id));

            org.springframework.http.client.JdkClientHttpRequestFactory factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
                    java.net.http.HttpClient.newBuilder()
                            .connectTimeout(java.time.Duration.ofMillis(properties.getTimeout()))
                            .build()
            );
            factory.setReadTimeout(java.time.Duration.ofMillis(properties.getTimeout()));

            return restClientBuilder
                    .baseUrl(instance.getAdminUrl())
                    .requestFactory(factory)
                    .build();
        });
    }

    public void evictClient(Long kongInstanceId) {
        clients.remove(kongInstanceId);
    }
}
