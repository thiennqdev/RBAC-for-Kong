package com.vcc.template.storage.service;

import com.vcc.template.storage.config.StorageProperties;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import io.minio.admin.MinioAdminClient;
import io.minio.admin.UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.vcc.template.storage.dto.StoragePolicyRuleDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageProvisioningService {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private MinioClient minioClient;
    private final StorageProperties storageProperties;

    // Helper class to return credentials
    public record StorageCredentials(String accessKey, String secretKey) {}

    private MinioAdminClient getAdminClient() {
        if (storageProperties.getEndpoint() == null || storageProperties.getEndpoint().isBlank()) {
            return null;
        }
        return MinioAdminClient.builder()
                .endpoint(storageProperties.getEndpoint())
                .credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey())
                .build();
    }

    public void createBucket(String bucketName) throws Exception {
        if (minioClient == null) return;
        
        log.info("Creating bucket: {}", bucketName);
        minioClient.makeBucket(
                MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .region(storageProperties.getRegion())
                        .build()
        );
    }

    public void applyBucketPolicy(String bucketName, boolean isPublic) throws Exception {
        if (minioClient == null) return;
        
        log.info("Applying policy to bucket {}. Public: {}", bucketName, isPublic);
        if (isPublic) {
            String policy = "{\n" +
                    "  \"Statement\": [\n" +
                    "    {\n" +
                    "      \"Action\": [\"s3:GetBucketLocation\", \"s3:ListBucket\"],\n" +
                    "      \"Effect\": \"Allow\",\n" +
                    "      \"Principal\": \"*\",\n" +
                    "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "\"]\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"Action\": [\"s3:GetObject\"],\n" +
                    "      \"Effect\": \"Allow\",\n" +
                    "      \"Principal\": \"*\",\n" +
                    "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"Version\": \"2012-10-17\"\n" +
                    "}";
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
        }
    }

    public void updateFolderPolicy(String bucketName, String folderPath, boolean isPublic) throws Exception {
        if (minioClient == null) return;
        
        String currentPolicy = "";
        try {
            currentPolicy = minioClient.getBucketPolicy(io.minio.GetBucketPolicyArgs.builder().bucket(bucketName).build());
        } catch (io.minio.errors.ErrorResponseException e) {
            if (!"NoSuchBucketPolicy".equals(e.errorResponse().code())) {
                throw e;
            }
        }
        
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode policyNode;
        if (currentPolicy == null || currentPolicy.isBlank()) {
            policyNode = mapper.createObjectNode();
            policyNode.put("Version", "2012-10-17");
            policyNode.putArray("Statement");
        } else {
            policyNode = (ObjectNode) mapper.readTree(currentPolicy);
        }
        
        ArrayNode statements = (ArrayNode) policyNode.get("Statement");
        if (statements == null) {
            statements = policyNode.putArray("Statement");
        }

        // Ensure ListBucket is allowed for anonymous users if we have any public folder
        ObjectNode listBucketStatement = null;
        ObjectNode getObjectStatement = null;

        for (JsonNode stmt : statements) {
            JsonNode actions = stmt.get("Action");
            if (actions != null && actions.isArray()) {
                for (JsonNode action : actions) {
                    if ("s3:GetBucketLocation".equals(action.asText()) || "s3:ListBucket".equals(action.asText())) {
                        listBucketStatement = (ObjectNode) stmt;
                    }
                    if ("s3:GetObject".equals(action.asText())) {
                        getObjectStatement = (ObjectNode) stmt;
                    }
                }
            }
        }

        if (listBucketStatement == null) {
            listBucketStatement = mapper.createObjectNode();
            ArrayNode actions = listBucketStatement.putArray("Action");
            actions.add("s3:GetBucketLocation");
            actions.add("s3:ListBucket");
            listBucketStatement.put("Effect", "Allow");
            listBucketStatement.put("Principal", "*");
            ArrayNode resources = listBucketStatement.putArray("Resource");
            resources.add("arn:aws:s3:::" + bucketName);
            statements.add(listBucketStatement);
        }

        if (getObjectStatement == null) {
            getObjectStatement = mapper.createObjectNode();
            ArrayNode actions = getObjectStatement.putArray("Action");
            actions.add("s3:GetObject");
            getObjectStatement.put("Effect", "Allow");
            getObjectStatement.put("Principal", "*");
            getObjectStatement.putArray("Resource");
            statements.add(getObjectStatement);
        }

        JsonNode resourceNode = getObjectStatement.get("Resource");
        ArrayNode getObjectResources;
        if (resourceNode == null) {
            getObjectResources = getObjectStatement.putArray("Resource");
        } else if (resourceNode.isTextual()) {
            getObjectResources = mapper.createArrayNode();
            getObjectResources.add(resourceNode.asText());
            getObjectStatement.set("Resource", getObjectResources);
        } else {
            getObjectResources = (ArrayNode) resourceNode;
        }

        String targetResource = "arn:aws:s3:::" + bucketName + "/" + folderPath + "/*";
        
        // Remove existing resource if present
        for (int i = getObjectResources.size() - 1; i >= 0; i--) {
            if (targetResource.equals(getObjectResources.get(i).asText())) {
                getObjectResources.remove(i);
            }
        }
        
        if (isPublic) {
            getObjectResources.add(targetResource);
        }

        if (getObjectResources.isEmpty()) {
            // Remove the getObjectStatement if no resources left to avoid MalformedPolicy
            for (int i = statements.size() - 1; i >= 0; i--) {
                if (statements.get(i) == getObjectStatement) {
                    statements.remove(i);
                }
            }
        }

        // If no statements left at all, we can just delete the policy or set an empty one.
        if (statements.isEmpty()) {
            minioClient.deleteBucketPolicy(io.minio.DeleteBucketPolicyArgs.builder().bucket(bucketName).build());
            return;
        }

        String updatedPolicy = mapper.writeValueAsString(policyNode);
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(updatedPolicy)
                        .build()
        );
    }

    public List<StoragePolicyRuleDto> getCustomPolicies(String bucketName) throws Exception {
        if (minioClient == null) return new ArrayList<>();

        String currentPolicy = "";
        try {
            currentPolicy = minioClient.getBucketPolicy(io.minio.GetBucketPolicyArgs.builder().bucket(bucketName).build());
        } catch (io.minio.errors.ErrorResponseException e) {
            if (!"NoSuchBucketPolicy".equals(e.errorResponse().code())) {
                throw e;
            }
        }

        if (currentPolicy == null || currentPolicy.isBlank()) {
            return new ArrayList<>();
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode policyNode = (ObjectNode) mapper.readTree(currentPolicy);
        ArrayNode statements = (ArrayNode) policyNode.get("Statement");
        if (statements == null) return new ArrayList<>();

        Map<String, StoragePolicyRuleDto> ruleMap = new HashMap<>();

        for (JsonNode stmt : statements) {
            String effect = stmt.path("Effect").asText();
            JsonNode principal = stmt.get("Principal");
            boolean isPublic = false;
            if (principal != null) {
                if (principal.isTextual() && "*".equals(principal.asText())) {
                    isPublic = true;
                } else if (principal.isObject()) {
                    JsonNode aws = principal.path("AWS");
                    if (aws.isTextual() && "*".equals(aws.asText())) {
                        isPublic = true;
                    } else if (aws.isArray() && aws.size() > 0 && "*".equals(aws.get(0).asText())) {
                        isPublic = true;
                    }
                }
            }
            if (!"Allow".equals(effect) || !isPublic) continue;

            List<String> actions = new ArrayList<>();
            JsonNode actionNode = stmt.get("Action");
            if (actionNode != null) {
                if (actionNode.isArray()) {
                    actionNode.forEach(a -> actions.add(a.asText()));
                } else if (actionNode.isTextual()) {
                    actions.add(actionNode.asText());
                }
            }

            boolean canRead = actions.contains("s3:GetObject");
            boolean canWrite = actions.contains("s3:PutObject");
            boolean canDelete = actions.contains("s3:DeleteObject");

            if (!canRead && !canWrite && !canDelete) continue;

            List<String> ipAddresses = new ArrayList<>();
            JsonNode condition = stmt.get("Condition");
            if (condition != null && condition.has("IpAddress")) {
                JsonNode ipNode = condition.get("IpAddress").get("aws:SourceIp");
                if (ipNode != null) {
                    if (ipNode.isArray()) {
                        ipNode.forEach(ip -> ipAddresses.add(ip.asText()));
                    } else if (ipNode.isTextual()) {
                        ipAddresses.add(ipNode.asText());
                    }
                }
            }

            JsonNode resourceNode = stmt.get("Resource");
            List<String> resources = new ArrayList<>();
            if (resourceNode != null) {
                if (resourceNode.isArray()) {
                    resourceNode.forEach(r -> resources.add(r.asText()));
                } else if (resourceNode.isTextual()) {
                    resources.add(resourceNode.asText());
                }
            }

            for (String res : resources) {
                String prefix = res.replace("arn:aws:s3:::" + bucketName, "");
                if (prefix.startsWith("/")) prefix = prefix.substring(1);
                if (prefix.endsWith("/*")) prefix = prefix.substring(0, prefix.length() - 2);
                if (prefix.equals("/*") || prefix.isEmpty()) prefix = "*";

                StoragePolicyRuleDto rule = ruleMap.computeIfAbsent(prefix, p -> StoragePolicyRuleDto.builder()
                        .prefix(p)
                        .ipAddresses(new ArrayList<>())
                        .build());
                if (canRead) rule.setCanRead(true);
                if (canWrite) rule.setCanWrite(true);
                if (canDelete) rule.setCanDelete(true);
                if (ipAddresses != null && !ipAddresses.isEmpty()) {
                    if (rule.getIpAddresses() == null) rule.setIpAddresses(new ArrayList<>());
                    for (String ip : ipAddresses) {
                        if (!rule.getIpAddresses().contains(ip)) rule.getIpAddresses().add(ip);
                    }
                }
            }
        }
        return new ArrayList<>(ruleMap.values());
    }

    public void setCustomPolicies(String bucketName, List<StoragePolicyRuleDto> rules) throws Exception {
        if (minioClient == null) return;
        
        if (rules == null || rules.isEmpty()) {
            try {
                minioClient.deleteBucketPolicy(io.minio.DeleteBucketPolicyArgs.builder().bucket(bucketName).build());
            } catch (io.minio.errors.ErrorResponseException e) {
                if (!"NoSuchBucketPolicy".equals(e.errorResponse().code())) {
                    throw e;
                }
            }
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode policyNode = mapper.createObjectNode();
        policyNode.put("Version", "2012-10-17");
        ArrayNode statements = policyNode.putArray("Statement");

        // Always add ListBucket
        ObjectNode listBucketStatement = mapper.createObjectNode();
        ArrayNode listActions = listBucketStatement.putArray("Action");
        listActions.add("s3:GetBucketLocation");
        listActions.add("s3:ListBucket");
        listBucketStatement.put("Effect", "Allow");
        listBucketStatement.put("Principal", "*");
        ArrayNode listResources = listBucketStatement.putArray("Resource");
        listResources.add("arn:aws:s3:::" + bucketName);
        statements.add(listBucketStatement);

        for (StoragePolicyRuleDto rule : rules) {
            if (!rule.isCanRead() && !rule.isCanWrite() && !rule.isCanDelete()) continue;

            ObjectNode ruleStatement = mapper.createObjectNode();
            ruleStatement.put("Effect", "Allow");
            ruleStatement.put("Principal", "*");

            ArrayNode actions = ruleStatement.putArray("Action");
            if (rule.isCanRead()) actions.add("s3:GetObject");
            if (rule.isCanWrite()) actions.add("s3:PutObject");
            if (rule.isCanDelete()) actions.add("s3:DeleteObject");

            String resourceArn = "arn:aws:s3:::" + bucketName;
            String p = rule.getPrefix();
            if (p == null || p.isEmpty() || "*".equals(p) || "/*".equals(p)) {
                resourceArn += "/*";
            } else {
                if (p.startsWith("/")) p = p.substring(1);
                if (p.endsWith("/*")) {
                    resourceArn += "/" + p;
                } else if (p.endsWith("/")) {
                    resourceArn += "/" + p + "*";
                } else {
                    resourceArn += "/" + p + "/*";
                }
            }
            ArrayNode resources = ruleStatement.putArray("Resource");
            resources.add(resourceArn);

            if (rule.getIpAddresses() != null && !rule.getIpAddresses().isEmpty()) {
                ObjectNode condition = ruleStatement.putObject("Condition");
                ObjectNode ipAddress = condition.putObject("IpAddress");
                ArrayNode sourceIps = ipAddress.putArray("aws:SourceIp");
                for (String ip : rule.getIpAddresses()) {
                    sourceIps.add(ip);
                }
            }

            statements.add(ruleStatement);
        }

        String updatedPolicy = mapper.writeValueAsString(policyNode);
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(updatedPolicy)
                        .build()
        );
    }

    public StorageCredentials createServiceAccountForBucket(String bucketName) throws Exception {
        MinioAdminClient adminClient = getAdminClient();
        if (adminClient == null) {
            log.warn("MinIO Admin Client not configured, returning dummy credentials.");
            return new StorageCredentials("dummy-access-key", "dummy-secret-key");
        }

        // In a real environment, we should create a policy for the bucket, then create a user, then attach the policy.
        // For simplicity in MinIO, we can create a user. The user access key must be unique, so we'll use a prefix.
        String accessKey = "ak-" + bucketName + "-" + UUID.randomUUID().toString().substring(0, 8);
        String secretKey = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        
        log.info("Creating MinIO user: {}", accessKey);
        adminClient.addUser(accessKey, UserInfo.Status.ENABLED, secretKey, null, null);
        
        // Define policy allowing full access ONLY to this bucket
        String policyName = "policy-" + bucketName;
        String policyContent = "{\n" +
                "  \"Version\": \"2012-10-17\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Action\": [\"s3:*\"],\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Resource\": [\n" +
                "        \"arn:aws:s3:::" + bucketName + "\",\n" +
                "        \"arn:aws:s3:::" + bucketName + "/*\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
                
        log.info("Adding policy {} to MinIO", policyName);
        adminClient.addCannedPolicy(policyName, policyContent);
        
        log.info("Attaching policy {} to user {}", policyName, accessKey);
        adminClient.setPolicy(accessKey, false, policyName);

        return new StorageCredentials(accessKey, secretKey);
    }

    public void deleteServiceAccount(String accessKey) {
        MinioAdminClient adminClient = getAdminClient();
        if (adminClient != null && accessKey != null) {
            try {
                log.info("Deleting MinIO user: {}", accessKey);
                adminClient.deleteUser(accessKey);
            } catch (Exception e) {
                log.warn("Failed to delete MinIO user {}: {}", accessKey, e.getMessage());
            }
        }
    }

    public void setBucketQuota(String bucketName, long quotaInBytes) throws Exception {
        MinioAdminClient adminClient = getAdminClient();
        if (adminClient == null) return;
        
        log.info("Setting quota for bucket {} to {} bytes", bucketName, quotaInBytes);
        // Admin API to set quota. (Requires minio-admin client support for quotas, if not available directly, we just log)
        try {
            // adminClient.setBucketQuota(bucketName, quotaInBytes, io.minio.admin.QuotaUnit.BYTES);
            log.warn("MinIO Admin Quota API usage is commented out due to QuotaUnit missing in this version");
        } catch (NoSuchMethodError e) {
            log.warn("setBucketQuota not available in this MinioAdminClient version. Skipping.");
        }
    }

    public void deleteStorage(String bucketName, String accessKey) {
        // 1. Delete MinIO user and policy
        MinioAdminClient adminClient = getAdminClient();
        if (adminClient != null) {
            try {
                if (accessKey != null) {
                    log.info("Deleting MinIO user: {}", accessKey);
                    adminClient.deleteUser(accessKey);
                }
                String policyName = "policy-" + bucketName;
                log.info("Removing policy: {}", policyName);
                adminClient.removeCannedPolicy(policyName);
            } catch (Exception e) {
                log.warn("Failed to delete MinIO user/policy for bucket {}: {}", bucketName, e.getMessage());
            }
        }

        // 2. Empty and delete the bucket
        if (minioClient != null) {
            try {
                log.info("Attempting to empty bucket: {}", bucketName);
                Iterable<io.minio.Result<io.minio.messages.Item>> results = minioClient.listObjects(
                        io.minio.ListObjectsArgs.builder().bucket(bucketName).recursive(true).build()
                );
                for (io.minio.Result<io.minio.messages.Item> result : results) {
                    io.minio.messages.Item item = result.get();
                    minioClient.removeObject(
                            io.minio.RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(item.objectName())
                                    .build()
                    );
                }

                log.info("Attempting to delete bucket: {}", bucketName);
                minioClient.removeBucket(io.minio.RemoveBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket {} deleted successfully.", bucketName);
            } catch (Exception e) {
                log.warn("Failed to empty/delete bucket {}: {}", bucketName, e.getMessage());
            }
        }
    }
}
