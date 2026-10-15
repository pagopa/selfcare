package it.pagopa.selfcare.security.tenant;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.container.ContainerRequestContext;

/** Reads the {@code X-Tenant-Id} header of a request in a form that is safe to log. */
public final class TenantLogUtils {

  private TenantLogUtils() {}

  public static String fromInboundRequest(ContainerRequestContext requestContext) {
    return sanitize(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER));
  }

  public static String fromClientRequest(ClientRequestContext requestContext) {
    return sanitize(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER));
  }

  private static String sanitize(String value) {
    if (value == null) {
      return "unknown";
    }
    return value.replaceAll("[^A-Za-z0-9_-]", "_");
  }
}
