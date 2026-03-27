package com.networknt.rule;

import com.networknt.utility.Util;

import java.util.Arrays;
import java.util.Map;

final class PermissionMatchUtils {

    private PermissionMatchUtils() {
    }

    static boolean hasAnyConfiguredPermission(String jwtPermissions, String configuredPermissions) {
        if (jwtPermissions == null || configuredPermissions == null) {
            return false;
        }
        return Arrays.stream(configuredPermissions.split(","))
                .map(String::trim)
                .filter(permission -> !permission.isEmpty())
                .anyMatch(permission -> hasPermission(jwtPermissions, permission));
    }

    static boolean hasPermission(String jwtPermissions, String requiredPermission) {
        if (jwtPermissions == null || requiredPermission == null) {
            return false;
        }
        String normalizedRequired = requiredPermission.trim();
        if (normalizedRequired.isEmpty()) {
            return false;
        }
        if (jwtPermissions.contains("^=^") || jwtPermissions.contains("~")) {
            Map<String, String> jwtAttributes = Util.parseAttributes(jwtPermissions);
            if (jwtAttributes.isEmpty()) {
                return normalizedRequired.equals(jwtPermissions.trim());
            }
            if (normalizedRequired.contains("^=^")) {
                Map<String, String> requiredAttributes = Util.parseAttributes(normalizedRequired);
                if (requiredAttributes.size() != 1) {
                    return false;
                }
                Map.Entry<String, String> entry = requiredAttributes.entrySet().iterator().next();
                return entry.getValue().equals(jwtAttributes.get(entry.getKey()));
            }
            return jwtAttributes.containsKey(normalizedRequired);
        }
        return Arrays.stream(jwtPermissions.split("\\s+"))
                .map(String::trim)
                .filter(permission -> !permission.isEmpty())
                .anyMatch(normalizedRequired::equals);
    }
}
