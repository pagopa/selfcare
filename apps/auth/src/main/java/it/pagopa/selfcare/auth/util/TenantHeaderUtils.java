package it.pagopa.selfcare.auth.util;

import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import it.pagopa.selfcare.security.tenant.TenantConstants;
import it.pagopa.selfcare.security.tenant.TenantId;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Resolves the tenant identity from the {@code X-Tenant-Id} header on session-issuing endpoints
 * (SAML callback, OIDC exchange, OTP verify), so it can be embedded as the {@code tenant_id} claim
 * of the JWT session token issued by {@code auth} (Step_0 SELC-4).
 *
 * <p>APIM always sets this header from the calling frontend's {@code Origin}/{@code Referer}
 * before forwarding (Step_0 SELC-1), so a missing or unknown value here means the request did not
 * come through APIM as expected; such requests are rejected rather than defaulted to a tenant
 * (fail-closed, consistent with {@code TenantValidationFilter} in {@code selfcare-sdk-security}).
 */
public final class TenantHeaderUtils {

  private TenantHeaderUtils() {}

  public static String resolveTenantId(ContainerRequestContext requestContext) {
    String headerValue = requestContext.getHeaderString(TenantConstants.TENANT_HEADER);
    return TenantId.fromValue(headerValue)
        .orElseThrow(
            () ->
                new InvalidRequestException(
                    "Missing or unknown "
                        + TenantConstants.TENANT_HEADER
                        + " header: request did not pass through APIM tenant resolution"))
        .name();
  }
}
