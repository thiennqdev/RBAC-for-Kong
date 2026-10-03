package com.vcc.template.observability.config;

import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.entity.KongInstanceEntity;
import com.vcc.template.kong.repository.KongInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KongObservabilityInitializer implements ApplicationRunner {

    private final KongAdminClient kongAdminClient;
    private final KongInstanceRepository kongInstanceRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking global Kong observability plugins for all instances...");
        
        List<KongInstanceEntity> instances = kongInstanceRepository.findAll();
        for (KongInstanceEntity instance : instances) {
            Long kongInstanceId = instance.getId();
            log.info("Checking instance: {}", instance.getName());
            try {
                Map<String, Object> pluginsResponse = kongAdminClient.listGlobalPlugins(kongInstanceId);
                List<Map<String, Object>> plugins = (List<Map<String, Object>>) pluginsResponse.get("data");

                boolean hasPrometheus = false;
                boolean hasHttpLog = false;

                if (plugins != null) {
                    for (Map<String, Object> plugin : plugins) {
                        String name = (String) plugin.get("name");
                        if ("prometheus".equals(name)) hasPrometheus = true;
                        if ("http-log".equals(name)) hasHttpLog = true;
                    }
                }

                if (!hasPrometheus) {
                    log.info("Provisioning global prometheus plugin on instance {}...", instance.getName());
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("name", "prometheus");
                    kongAdminClient.createGlobalPlugin(kongInstanceId, payload);
                }

                if (!hasHttpLog) {
                    log.info("Provisioning global http-log plugin on instance {}...", instance.getName());
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("name", "http-log");
                    
                    Map<String, Object> config = new HashMap<>();
                    config.put("http_endpoint", "http://host.docker.internal:8080/api/internal/kong-logs");
                    config.put("method", "POST");
                    config.put("timeout", 10000);
                    config.put("keepalive", 60000);
                    payload.put("config", config);
                    
                    kongAdminClient.createGlobalPlugin(kongInstanceId, payload);
                }

                log.info("Kong global observability plugins are ready for instance {}.", instance.getName());

            } catch (Exception e) {
                log.error("Failed to initialize Kong global plugins for instance {}: {}", instance.getName(), e.getMessage(), e);
            }
        }
    }
}
