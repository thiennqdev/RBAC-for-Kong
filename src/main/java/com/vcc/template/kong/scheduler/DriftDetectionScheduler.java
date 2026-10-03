package com.vcc.template.kong.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.entity.KongServiceEntity;
import com.vcc.template.kong.entity.KongRouteEntity;
import com.vcc.template.kong.entity.SyncStatus;
import com.vcc.template.kong.repository.KongRouteRepository;
import com.vcc.template.kong.repository.KongServiceRepository;
import com.vcc.template.kong.repository.KongUpstreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "kong.drift-detection.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DriftDetectionScheduler {

    private final KongServiceRepository serviceRepository;
    private final KongRouteRepository routeRepository;
    private final KongUpstreamRepository upstreamRepository;
    private final KongAdminClient kongAdminClient;

    @Scheduled(fixedRateString = "${kong.drift-detection.interval:300000}")
    public void detectDrift() {
        log.info("Starting Kong drift detection...");

        List<KongServiceEntity> services = serviceRepository.findAll();
        for (KongServiceEntity service : services) {
            try {
                Map kongData = kongAdminClient.getService(service.getKongInstance().getId(), service.getProject().getProjectCode(), service.getKongId());
                // Sync OK
                if (service.getSyncStatus() != SyncStatus.SYNCED) {
                    service.setSyncStatus(SyncStatus.SYNCED);
                    serviceRepository.save(service);
                }
            } catch (Exception e) {
                log.warn("Drift detected: Service {} is out of sync or missing in Kong", service.getKongId());
                service.setSyncStatus(SyncStatus.OUT_OF_SYNC);
                serviceRepository.save(service);
            }
        }

        // Check Routes
        List<KongRouteEntity> routes = routeRepository.findAll();
        for (KongRouteEntity route : routes) {
            try {
                Map kongData = kongAdminClient.getRoute(route.getKongInstance().getId(), route.getProject().getProjectCode(), route.getKongId());
                if (route.getSyncStatus() != SyncStatus.SYNCED) {
                    route.setSyncStatus(SyncStatus.SYNCED);
                    routeRepository.save(route);
                }
            } catch (Exception e) {
                log.warn("Drift detected: Route {} is out of sync or missing in Kong", route.getKongId());
                route.setSyncStatus(SyncStatus.OUT_OF_SYNC);
                routeRepository.save(route);
            }
        }

        // Similarly for upstreams
        log.info("Completed Kong drift detection.");
    }
}
