package it.pagopa.selfcare.document.filter;

import jakarta.ws.rs.container.ContainerRequestContext;

/** Sanitizes tenant values coming from untrusted headers before they reach logs. */
public final class TenantLogUtils {

  static final String UNKNOWN = "unknown";

  private TenantLogUtils() {}

  public static String fromInboundRequest(ContainerRequestContext requestContext) {
    return sanitize(requestContext.getHeaderString(TenantResolutionFilter.TENANT_HEADER));
  }

  public static String sanitize(String value) {
    if (value == null || value.isBlank()) {
      return UNKNOWN;
    }
    String sanitized = value.replaceAll("[^A-Za-z0-9_-]", "_");
    return sanitized.length() > 32 ? sanitized.substring(0, 32) : sanitized;
  }
}
