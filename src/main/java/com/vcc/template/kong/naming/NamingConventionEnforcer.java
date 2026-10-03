package com.vcc.template.kong.naming;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NamingConventionEnforcer {

    @Value("${kong.naming.separator:.}")
    private String separator;

    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (!name.matches("^[a-zA-Z0-9-]+$")) {
            throw new IllegalArgumentException("Name can only contain alphanumeric characters and hyphens, no spaces or special characters");
        }
    }

    public String formatName(String projectCode, String userInputName) {
        validateName(userInputName);
        return projectCode + separator + userInputName;
    }

    public String extractUserFriendlyName(String fullName, String projectCode) {
        String prefix = projectCode + separator;
        if (fullName != null && fullName.startsWith(prefix)) {
            return fullName.substring(prefix.length());
        }
        return fullName;
    }

    /**
     * Auto-prefix a single path with /{projectCode}.
     * Ensures the result always starts with /{projectCode}/.
     * <p>
     * Examples:
     * <ul>
     *   <li>formatPath("shopee", "/orders")  → "/shopee/orders"</li>
     *   <li>formatPath("shopee", "orders")   → "/shopee/orders"</li>
     *   <li>formatPath("shopee", "/v1/orders") → "/shopee/v1/orders"</li>
     * </ul>
     */
    public String formatPath(String projectCode, String userInputPath) {
        if (userInputPath == null || userInputPath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }
        String cleanPath = userInputPath.startsWith("/") ? userInputPath : "/" + userInputPath;
        
        String expectedPrefix = "/" + projectCode;
        if (cleanPath.equals(expectedPrefix) || cleanPath.startsWith(expectedPrefix + "/")) {
            return cleanPath;
        }
        
        return expectedPrefix + cleanPath;
    }

    /**
     * Auto-prefix a list of paths with /{projectCode}.
     */
    public List<String> formatPaths(String projectCode, List<String> userInputPaths) {
        if (userInputPaths == null || userInputPaths.isEmpty()) {
            return userInputPaths;
        }
        return userInputPaths.stream()
                .map(path -> formatPath(projectCode, path))
                .toList();
    }
}
