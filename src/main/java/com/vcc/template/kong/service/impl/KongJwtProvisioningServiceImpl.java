package com.vcc.template.kong.service.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.vcc.template.kong.client.KongAdminClient;
import com.vcc.template.kong.service.KongJwtProvisioningService;
import com.vcc.template.project.entity.Project;
import com.vcc.template.project.errorcode.ProjectErrorCode;
import com.vcc.template.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of KongJwtProvisioningService.
 *
 * <p>Handles the full lifecycle of JWT authentication on Kong Gateway:
 * <ol>
 *   <li>Reads IdP configuration from the project (DEFAULT or CUSTOM)</li>
 *   <li>Fetches JWKS public keys from the configured IdP endpoint</li>
 *   <li>Creates a Kong Consumer and registers JWT credentials</li>
 *   <li>Enables the JWT plugin on Kong Services/Routes</li>
 * </ol>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KongJwtProvisioningServiceImpl implements KongJwtProvisioningService {

    private final KongAdminClient kongAdminClient;
    private final ProjectRepository projectRepository;
    private final com.vcc.template.kong.repository.KongInstanceRepository kongInstanceRepository;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.server-url:http://localhost:8180}")
    private String defaultKeycloakUrl;

    @Value("${keycloak.auth.realm:rbac-management}")
    private String defaultRealm;

    @Value("${keycloak.auth.client-id:rbac-admin-frontend}")
    private String defaultClientId;

    @Override
    public Map<String, Object> provisionJwtForProject(Long kongInstanceId, String projectCode) {
        log.info("Provisioning JWT authentication for project: {} on instance: {}", projectCode, kongInstanceId);

        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode));

        // 1. Determine JWKS URL based on IdP config
        String jwksUrl = resolveJwksUrl(project);
        log.debug("Using JWKS URL for project {}: {}", projectCode, jwksUrl);

        // 2. Fetch the first RSA public key from JWKS
        Map<String, String> keyInfo = fetchRsaPublicKeyFromJwks(jwksUrl);
        String kid = keyInfo.get("kid");
        String algorithm = keyInfo.get("algorithm");
        String rsaPublicKey = keyInfo.get("publicKey");

        log.debug("Fetched RSA key: kid={}, algorithm={}", kid, algorithm);

        // 3. Create a Shared Kong Consumer for this Client ID
        // Instead of a consumer per project, we use one consumer per Client ID to avoid JWT 'key' conflicts
        // and to allow strict ACL isolation between different Client IDs on the same Realm.
        String jwtKey = (project.getIdpClientId() != null && !project.getIdpClientId().isBlank())
                ? project.getIdpClientId()
                : defaultClientId;

        String consumerUsername = "idp-client-" + jwtKey;
        Map<String, Object> consumerPayload = new HashMap<>();
        consumerPayload.put("username", consumerUsername);
        consumerPayload.put("tags", List.of("shared-idp", "client:" + jwtKey));

        Map consumerResult = null;
        try {
            String adminUrl = resolveKongAdminUrl(kongInstanceId);
            RestClient kongClient = restClientBuilder.baseUrl(adminUrl).build();
            consumerResult = kongClient.put()
                    .uri("/consumers/{username}", consumerUsername)
                    .body(consumerPayload)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.warn("Shared IdP Kong Consumer {} may already exist: {}", consumerUsername, e.getMessage());
        }

        // 4. Add JWT credential to the Shared Consumer
        Map<String, Object> jwtPayload = new HashMap<>();
        jwtPayload.put("key", jwtKey);
        jwtPayload.put("algorithm", algorithm);
        jwtPayload.put("rsa_public_key", rsaPublicKey);

        try {
            String adminUrl = resolveKongAdminUrl(kongInstanceId);
            RestClient kongClient = restClientBuilder.baseUrl(adminUrl).build();
            kongClient.post()
                    .uri("/consumers/{username}/jwt", consumerUsername)
                    .body(jwtPayload)
                    .retrieve()
                    .body(Map.class);
            log.info("Provisioned new JWT credential for shared IdP: {}", consumerUsername);
        } catch (Exception e) {
            log.info("JWT credential for IdP {} already exists (Shared across projects).", consumerUsername);
        }

        // 5. Add consumer to ACL group based on Client ID
        String aclGroup = getAclGroupForProject(projectCode);
        Map<String, Object> aclPayload = new HashMap<>();
        aclPayload.put("group", aclGroup);
        try {
            String adminUrl = resolveKongAdminUrl(kongInstanceId);
            RestClient kongClient = restClientBuilder.baseUrl(adminUrl).build();
            kongClient.post()
                    .uri("/consumers/{username}/acls", consumerUsername)
                    .body(aclPayload)
                    .retrieve()
                    .body(Map.class);
            log.info("Assigned consumer {} to ACL group: {}", consumerUsername, aclGroup);
        } catch (Exception e) {
            log.info("Consumer {} is already in ACL group {} (Shared across projects).", consumerUsername, aclGroup);
        }

        log.info("JWT provisioning completed for project: {} (mapped to shared IdP {})", projectCode, consumerUsername);
        return consumerResult != null ? consumerResult : Map.of("username", consumerUsername, "status", "provisioned");
    }

    private String extractRealmName(String jwksUrl) {
        try {
            String[] parts = jwksUrl.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("realms".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "default";
    }

    @Override
    public Map<String, Object> enableJwtPlugin(Long kongInstanceId, String projectCode, String entityType, String entityId) {
        log.info("Enabling JWT plugin on {} {} for project {} instance {}", entityType, entityId, projectCode, kongInstanceId);

        Map<String, Object> pluginPayload = new HashMap<>();
        pluginPayload.put("name", "jwt");
        pluginPayload.put("tags", List.of("project_code:" + projectCode));

        Map<String, Object> config = new HashMap<>();
        config.put("claims_to_verify", List.of("exp"));
        
        // Instruct Kong to extract the key from the 'azp' (Authorized Party) claim, which holds the Keycloak Client ID.
        // We ALWAYS use 'azp' now to prevent Cross-Client Token usage on the same Realm.
        config.put("key_claim_name", "azp");
        
        pluginPayload.put("config", config);

        try {
            String adminUrl = resolveKongAdminUrl(kongInstanceId);
            RestClient kongClient = restClientBuilder.baseUrl(adminUrl).build();
            Map result = kongClient.post()
                    .uri("/{entityType}/{entityId}/plugins", entityType, entityId)
                    .body(pluginPayload)
                    .retrieve()
                    .body(Map.class);

            log.info("JWT plugin enabled on {} {} for project {}", entityType, entityId, projectCode);
            return result;
        } catch (Exception e) {
            log.error("Failed to enable JWT plugin: {}", e.getMessage());
            throw new RuntimeException("Failed to enable JWT plugin: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> enableAclPlugin(Long kongInstanceId, String projectCode, String entityType, String entityId, String allowedGroup) {
        log.info("Enabling ACL plugin on {} {} for project {} instance {} with allowed group: {}", entityType, entityId, projectCode, kongInstanceId, allowedGroup);

        Map<String, Object> pluginPayload = new HashMap<>();
        pluginPayload.put("name", "acl");
        pluginPayload.put("tags", List.of("project_code:" + projectCode));

        Map<String, Object> config = new HashMap<>();
        config.put("allow", List.of(allowedGroup));
        pluginPayload.put("config", config);

        try {
            String adminUrl = resolveKongAdminUrl(kongInstanceId);
            RestClient kongClient = restClientBuilder.baseUrl(adminUrl).build();
            Map result = kongClient.post()
                    .uri("/{entityType}/{entityId}/plugins", entityType, entityId)
                    .body(pluginPayload)
                    .retrieve()
                    .body(Map.class);

            log.info("ACL plugin enabled on {} {} for project {}", entityType, entityId, projectCode);
            return result;
        } catch (Exception e) {
            log.error("Failed to enable ACL plugin: {}", e.getMessage());
            throw new RuntimeException("Failed to enable ACL plugin: " + e.getMessage(), e);
        }
    }

    @Override
    public String getAclGroupForProject(String projectCode) {
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode));
        String jwtKey = (project.getIdpClientId() != null && !project.getIdpClientId().isBlank())
                ? project.getIdpClientId()
                : defaultClientId;
        return "group-client-" + jwtKey;
    }

    @Override
    public void deprovisionJwtForProject(Long kongInstanceId, String projectCode) {
        log.info("Deprovisioning JWT for project: {} on instance {} (Skipped because IdP consumer is shared across projects)", projectCode, kongInstanceId);
        // We no longer delete the consumer because it is named `idp-realm-rbac-management`
        // and shared by all projects using this realm. Deleting it would break authentication
        // for all other projects.
    }

    @Override
    public void syncJwtAuthForProject(String projectCode, boolean enabled) {
        log.info("Syncing JWT Auth for project {} with enabled={}", projectCode, enabled);
        Project project = projectRepository.findByProjectCode(projectCode)
                .orElseThrow(() -> ProjectErrorCode.PROJECT_NOT_FOUND.toException(projectCode));
        
        for (com.vcc.template.kong.entity.KongInstanceEntity instance : kongInstanceRepository.findAll()) {
            Long instanceId = instance.getId();
            
            Map response = null;
            try {
                response = kongAdminClient.listPlugins(instanceId, projectCode);
            } catch (Exception e) {
                log.error("Failed to list plugins on instance {}: {}", instanceId, e.getMessage());
                continue;
            }
            
            List<Map<String, Object>> plugins = (List<Map<String, Object>>) response.get("data");
            if (plugins == null) continue;
            
            if (!enabled) {
                // Delete all 'jwt' and 'acl' plugins
                for (Map<String, Object> plugin : plugins) {
                    String name = (String) plugin.get("name");
                    if ("jwt".equals(name) || "acl".equals(name)) {
                        String pluginId = (String) plugin.get("id");
                        try {
                            kongAdminClient.deletePlugin(instanceId, projectCode, pluginId);
                            log.info("Deleted auth plugin {} ({}) for project {}", name, pluginId, projectCode);
                        } catch (Exception e) {
                            log.error("Failed to delete plugin {}: {}", pluginId, e.getMessage());
                        }
                    }
                }
            } else {
                // Add 'jwt' and 'acl' to all services that don't have them
                Map servicesResponse = null;
                try {
                    servicesResponse = kongAdminClient.listServices(instanceId, projectCode);
                } catch (Exception e) {
                    log.error("Failed to list services on instance {}: {}", instanceId, e.getMessage());
                    continue;
                }
                
                List<Map<String, Object>> services = (List<Map<String, Object>>) servicesResponse.get("data");
                if (services == null) continue;
                
                String aclGroup = getAclGroupForProject(projectCode);
                
                for (Map<String, Object> service : services) {
                    String serviceId = (String) service.get("id");
                    
                    boolean hasJwt = plugins.stream().anyMatch(p -> "jwt".equals(p.get("name")) && p.get("service") != null && serviceId.equals(((Map)p.get("service")).get("id")));
                    boolean hasAcl = plugins.stream().anyMatch(p -> "acl".equals(p.get("name")) && p.get("service") != null && serviceId.equals(((Map)p.get("service")).get("id")));
                    
                    if (!hasJwt) {
                        try {
                            enableJwtPlugin(instanceId, projectCode, "services", serviceId);
                        } catch (Exception e) {
                            log.error("Error enabling JWT on service {}: {}", serviceId, e.getMessage());
                        }
                    }
                    if (!hasAcl) {
                        try {
                            enableAclPlugin(instanceId, projectCode, "services", serviceId, aclGroup);
                        } catch (Exception e) {
                            log.error("Error enabling ACL on service {}: {}", serviceId, e.getMessage());
                        }
                    }
                }
            }
        }
    }

    // ==================== Private Helpers ====================

    private String resolveJwksUrl(Project project) {
        if (project.getIdpJwksUrl() != null && !project.getIdpJwksUrl().isBlank()
                && !"DEFAULT".equals(project.getIdpType())) {
            return project.getIdpJwksUrl();
        }
        // Default: use system Keycloak realm
        return defaultKeycloakUrl + "/realms/" + defaultRealm + "/protocol/openid-connect/certs";
    }

    private String resolveIssuerUrl(Project project) {
        if (project.getIdpIssuerUrl() != null && !project.getIdpIssuerUrl().isBlank()
                && !"DEFAULT".equals(project.getIdpType())) {
            return project.getIdpIssuerUrl();
        }
        return defaultKeycloakUrl + "/realms/" + defaultRealm;
    }

    private String resolveKongAdminUrl(Long kongInstanceId) {
        com.vcc.template.kong.entity.KongInstanceEntity instance = kongInstanceRepository.findById(kongInstanceId)
                .orElseThrow(() -> new IllegalArgumentException("Kong instance not found: " + kongInstanceId));
        return instance.getAdminUrl();
    }

    /**
     * Fetches the first RSA public key from a JWKS endpoint.
     * Returns a map with kid, algorithm, and PEM-encoded public key.
     */
    private Map<String, String> fetchRsaPublicKeyFromJwks(String jwksUrl) {
        try {
            RestClient client = restClientBuilder.build();
            String jwksJson = client.get()
                    .uri(jwksUrl)
                    .retrieve()
                    .body(String.class);

            JsonNode jwks = objectMapper.readTree(jwksJson);
            JsonNode keys = jwks.get("keys");

            if (keys == null || !keys.isArray() || keys.isEmpty()) {
                throw new RuntimeException("No keys found in JWKS response from " + jwksUrl);
            }

            // Find the first RSA key used for signing
            JsonNode rsaKey = null;
            for (JsonNode key : keys) {
                String kty = key.has("kty") ? key.get("kty").asText() : "";
                String use = key.has("use") ? key.get("use").asText() : "sig";
                if ("RSA".equals(kty) && "sig".equals(use)) {
                    rsaKey = key;
                    break;
                }
            }

            if (rsaKey == null) {
                // Fallback: use first key
                rsaKey = keys.get(0);
            }

            String kid = rsaKey.has("kid") ? rsaKey.get("kid").asText() : "default";
            String algorithm = rsaKey.has("alg") ? rsaKey.get("alg").asText() : "RS256";

            // Convert JWK to PEM public key
            String n = rsaKey.get("n").asText();
            String e = rsaKey.get("e").asText();
            String pemKey = jwkToPem(n, e);

            Map<String, String> result = new HashMap<>();
            result.put("kid", kid);
            result.put("algorithm", algorithm);
            result.put("publicKey", pemKey);
            return result;

        } catch (Exception e) {
            log.error("Failed to fetch JWKS from {}: {}", jwksUrl, e.getMessage());
            throw new RuntimeException("Failed to fetch JWKS public key: " + e.getMessage(), e);
        }
    }

    /**
     * Converts JWK RSA key components (n, e) to a PEM-encoded public key.
     */
    private String jwkToPem(String n, String e) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(n);
            byte[] eBytes = Base64.getUrlDecoder().decode(e);

            java.security.spec.RSAPublicKeySpec spec = new java.security.spec.RSAPublicKeySpec(
                    new java.math.BigInteger(1, nBytes),
                    new java.math.BigInteger(1, eBytes)
            );

            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.PublicKey publicKey = keyFactory.generatePublic(spec);

            byte[] encoded = publicKey.getEncoded();
            String base64 = Base64.getEncoder().encodeToString(encoded);

            StringBuilder pem = new StringBuilder();
            pem.append("-----BEGIN PUBLIC KEY-----\n");
            for (int i = 0; i < base64.length(); i += 64) {
                pem.append(base64, i, Math.min(i + 64, base64.length()));
                pem.append("\n");
            }
            pem.append("-----END PUBLIC KEY-----");
            return pem.toString();

        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert JWK to PEM: " + ex.getMessage(), ex);
        }
    }
}
