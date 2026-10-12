package it.pagopa.selfcare.onboarding.security;

import java.util.List;
import java.util.Set;

/** Paths that the Spring BFF excludes from authentication. */
final class SecurityPaths {

    private static final Set<String> EXACT = Set.of("/v3/api-docs", "/swagger-ui.html", "/favicon.ico", "/error");

    private static final List<String> SUBTREES =
            List.of("/swagger-resources", "/v3/api-docs", "/swagger-ui", "/actuator", "/dapr");

    private SecurityPaths() {
    }

    static boolean isPublic(String path) {
        if (path == null) {
            return false;
        }
        return EXACT.contains(path)
                || SUBTREES.stream().anyMatch(root -> path.equals(root) || path.startsWith(root + "/"));
    }
}
