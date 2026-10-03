package com.vcc.template.kong.client;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class KongAdminClient {
    private final KongClientFactory kongClientFactory;

    private void injectTag(Map<String, Object> payload, String projectCode) {
        String tag = "project_code:" + projectCode;
        List<String> tags = new ArrayList<>();
        tags.add(tag);
        
        if (payload.containsKey("tags")) {
            Object existingTags = payload.get("tags");
            if (existingTags instanceof List) {
                tags.addAll((List<String>) existingTags);
            }
        }
        payload.put("tags", tags);
    }

    public Map createService(Long kongInstanceId, String projectCode, Map<String, Object> payload) {
        injectTag(payload, projectCode);
        return kongClientFactory.getClient(kongInstanceId).post()
                .uri("/services")
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public Map getService(Long kongInstanceId, String projectCode, String serviceId) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri("/services/{id}", serviceId)
                .retrieve()
                .body(Map.class);
    }

    public Map listServices(Long kongInstanceId, String projectCode) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/services")
                        .queryParam("tags", "project_code:" + projectCode)
                        .build())
                .retrieve()
                .body(Map.class);
    }

    public Map updateService(Long kongInstanceId, String projectCode, String serviceId, Map<String, Object> payload) {
        injectTag(payload, projectCode);
        return kongClientFactory.getClient(kongInstanceId).patch()
                .uri("/services/{id}", serviceId)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public void deleteService(Long kongInstanceId, String projectCode, String serviceId) {
        kongClientFactory.getClient(kongInstanceId).delete()
                .uri("/services/{id}", serviceId)
                .retrieve()
                .toBodilessEntity();
    }

    // Routes
    public Map createRoute(Long kongInstanceId, String projectCode, String serviceId, Map<String, Object> payload) {
        injectTag(payload, projectCode);
        // Sometimes route doesn't need service in path but we can use /services/{id}/routes
        return kongClientFactory.getClient(kongInstanceId).post()
                .uri("/services/{serviceId}/routes", serviceId)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public Map getRoute(Long kongInstanceId, String projectCode, String routeId) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri("/routes/{id}", routeId)
                .retrieve()
                .body(Map.class);
    }

    public Map listRoutes(Long kongInstanceId, String projectCode, String serviceId) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/services/{serviceId}/routes")
                        .queryParam("tags", "project_code:" + projectCode)
                        .build(serviceId))
                .retrieve()
                .body(Map.class);
    }

    public Map listAllRoutes(Long kongInstanceId, String projectCode) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/routes")
                        .queryParam("tags", "project_code:" + projectCode)
                        .build())
                .retrieve()
                .body(Map.class);
    }

    public Map updateRoute(Long kongInstanceId, String projectCode, String routeId, Map<String, Object> payload) {
        injectTag(payload, projectCode);
        return kongClientFactory.getClient(kongInstanceId).patch()
                .uri("/routes/{id}", routeId)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public void deleteRoute(Long kongInstanceId, String projectCode, String routeId) {
        kongClientFactory.getClient(kongInstanceId).delete()
                .uri("/routes/{id}", routeId)
                .retrieve()
                .toBodilessEntity();
    }

    // Upstreams
    public Map createUpstream(Long kongInstanceId, String projectCode, Map<String, Object> payload) {
        injectTag(payload, projectCode);
        return kongClientFactory.getClient(kongInstanceId).post()
                .uri("/upstreams")
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public Map getUpstream(Long kongInstanceId, String projectCode, String upstreamId) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri("/upstreams/{id}", upstreamId)
                .retrieve()
                .body(Map.class);
    }

    public Map listUpstreams(Long kongInstanceId, String projectCode) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/upstreams")
                        .queryParam("tags", "project_code:" + projectCode)
                        .build())
                .retrieve()
                .body(Map.class);
    }

    public Map updateUpstream(Long kongInstanceId, String projectCode, String upstreamId, Map<String, Object> payload) {
        injectTag(payload, projectCode);
        return kongClientFactory.getClient(kongInstanceId).patch()
                .uri("/upstreams/{id}", upstreamId)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public void deleteUpstream(Long kongInstanceId, String projectCode, String upstreamId) {
        kongClientFactory.getClient(kongInstanceId).delete()
                .uri("/upstreams/{id}", upstreamId)
                .retrieve()
                .toBodilessEntity();
    }

    // Plugins
    public Map createPlugin(Long kongInstanceId, String projectCode, Map<String, Object> payload) {
        injectTag(payload, projectCode);
        return kongClientFactory.getClient(kongInstanceId).post()
                .uri("/plugins")
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public Map getPlugin(Long kongInstanceId, String projectCode, String pluginId) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri("/plugins/{id}", pluginId)
                .retrieve()
                .body(Map.class);
    }

    public Map listPlugins(Long kongInstanceId, String projectCode) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/plugins")
                        .queryParam("tags", "project_code:" + projectCode)
                        .build())
                .retrieve()
                .body(Map.class);
    }

    public Map updatePlugin(Long kongInstanceId, String projectCode, String pluginId, Map<String, Object> payload) {
        injectTag(payload, projectCode);
        return kongClientFactory.getClient(kongInstanceId).patch()
                .uri("/plugins/{id}", pluginId)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public void deletePlugin(Long kongInstanceId, String projectCode, String pluginId) {
        kongClientFactory.getClient(kongInstanceId).delete()
                .uri("/plugins/{id}", pluginId)
                .retrieve()
                .toBodilessEntity();
    }

    public Map listGlobalPlugins(Long kongInstanceId) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri("/plugins")
                .retrieve()
                .body(Map.class);
    }

    public Map createGlobalPlugin(Long kongInstanceId, Map<String, Object> payload) {
        return kongClientFactory.getClient(kongInstanceId).post()
                .uri("/plugins")
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    // Targets (nested under Upstreams)
    public Map addTarget(Long kongInstanceId, String upstreamKongId, Map<String, Object> payload) {
        return kongClientFactory.getClient(kongInstanceId).post()
                .uri("/upstreams/{upstreamId}/targets", upstreamKongId)
                .body(payload)
                .retrieve()
                .body(Map.class);
    }

    public Map listTargets(Long kongInstanceId, String upstreamKongId) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri("/upstreams/{upstreamId}/targets", upstreamKongId)
                .retrieve()
                .body(Map.class);
    }

    public Map getUpstreamHealth(Long kongInstanceId, String upstreamKongId) {
        return kongClientFactory.getClient(kongInstanceId).get()
                .uri("/upstreams/{upstreamId}/health", upstreamKongId)
                .retrieve()
                .body(Map.class);
    }

    public void deleteTarget(Long kongInstanceId, String upstreamKongId, String targetKongId) {
        kongClientFactory.getClient(kongInstanceId).delete()
                .uri("/upstreams/{upstreamId}/targets/{targetId}", upstreamKongId, targetKongId)
                .retrieve()
                .toBodilessEntity();
    }
}
