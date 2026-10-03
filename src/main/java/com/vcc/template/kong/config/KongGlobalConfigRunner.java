package com.vcc.template.kong.config;

import com.vcc.template.kong.client.KongAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ensures essential global plugins are enabled on Kong Gateway at application startup.
 *
 * <p>Currently handles:</p>
 * <ul>
 *     <li><b>prometheus</b> — Exposes Kong metrics for Prometheus scraping,
 *     enabling the Grafana monitoring dashboard (Bài toán 3: Observability).</li>
 * </ul>
 *
 * <p>This runner is idempotent: it checks whether the plugin is already enabled
 * before attempting to create it, so it is safe to run on every startup.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(100) // Run after database migrations but don't block critical startup
public class KongGlobalConfigRunner implements CommandLineRunner {

    private final KongAdminClient kongAdminClient;

    @Override
    public void run(String... args) {
        ensurePrometheusPlugin();
    }

    /**
     * Ensures the prometheus plugin is enabled globally on Kong.
     * If already present, this is a no-op.
     */
    @SuppressWarnings("unchecked")
    private void ensurePrometheusPlugin() {
        try {
            // 1. Check if prometheus plugin is already enabled globally
            Map<String, Object> pluginsResponse = kongAdminClient.listGlobalPlugins(1L); // FIXME: should iterate over instances
            List<Map<String, Object>> data = (List<Map<String, Object>>) pluginsResponse.get("data");

            if (data != null) {
                boolean prometheusExists = data.stream()
                        .anyMatch(plugin -> "prometheus".equals(plugin.get("name")));

                if (prometheusExists) {
                    log.info("Kong prometheus plugin is already enabled globally. Skipping.");
                    return;
                }
            }

            // 2. Enable prometheus plugin globally
            Map<String, Object> payload = new HashMap<>();
            payload.put("name", "prometheus");
            payload.put("config", Map.of(
                    "per_consumer", true,
                    "status_code_metrics", true,
                    "latency_metrics", true,
                    "bandwidth_metrics", true,
                    "upstream_health_metrics", true
            ));
            payload.put("tags", List.of("auto-provisioned", "observability"));

            kongAdminClient.createGlobalPlugin(1L, payload); // FIXME
            log.info("Kong prometheus plugin enabled globally for observability monitoring.");

        } catch (Exception e) {
            log.warn("Failed to ensure prometheus plugin on Kong Gateway (Kong may not be running yet): {}",
                    e.getMessage());
            // Don't throw — this should not prevent application startup.
            // The plugin can be enabled manually or will be retried on next restart.
        }
    }
}
