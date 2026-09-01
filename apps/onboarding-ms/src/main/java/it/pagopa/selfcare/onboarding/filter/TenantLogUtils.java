package it.pagopa.selfcare.onboarding.filter;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.container.ContainerRequestContext;

final class TenantLogUtils {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private TenantLogUtils() {}

    static String fromInboundRequest(ContainerRequestContext requestContext) {
        return sanitize(requestContext.getHeaderString(TENANT_HEADER));
    }

    static String fromClientRequest(ClientRequestContext requestContext) {
        return sanitize(requestContext.getHeaderString(TENANT_HEADER));
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
