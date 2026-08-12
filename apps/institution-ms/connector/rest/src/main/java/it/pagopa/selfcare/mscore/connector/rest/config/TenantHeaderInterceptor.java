package it.pagopa.selfcare.mscore.connector.rest.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Propagates the tenant onto outgoing service-to-service calls (Step_0 SELC-5).
 *
 * <p>Forwarding the JWT alone is not enough once downstream services enforce tenant consistency:
 * their {@code TenantValidationFilter} rejects with 400 any request that carries a JWT but no
 * {@code X-Tenant-Id} header. The clients configured with {@code AuthorizationHeaderInterceptor}
 * here target {@code user-ms} and {@code registry-proxy}, both of which enforce it, so this
 * interceptor is paired with it on the same clients.
 *
 * <p>The value is read from the request attribute set by this service's
 * {@code TenantValidationFilter} - the <b>validated</b> tenant, already reconciled against the JWT
 * claim - never echoed from the raw incoming header, which would forward unvalidated input if the
 * filter were ever bypassed for a path.
 *
 * <p>The attribute is typed as an enum rather than a {@code String} and is accepted only as such:
 * that is what rules out a value planted under the same attribute name. The enum class itself lives
 * in the {@code web} module, which this {@code connector/rest} module must not depend on (the
 * dependency runs the other way), hence the {@link Enum} handling instead of a direct type check.
 *
 * <p>Outside an active request (scheduled work, async threads without the request context) no
 * header is added, mirroring how the Authorization header degrades: a guessed tenant would be worse
 * than none.
 */
public class TenantHeaderInterceptor implements RequestInterceptor {

  /** Mirrors {@code TenantConstants#TENANT_REQUEST_ATTRIBUTE} in the {@code web} module. */
  static final String TENANT_REQUEST_ATTRIBUTE = "validatedTenantId";

  /** Mirrors {@code TenantConstants#TENANT_HEADER} in the {@code web} module. */
  static final String TENANT_HEADER = "X-Tenant-Id";

  @Override
  public void apply(RequestTemplate template) {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return;
    }
    Object tenant =
        attributes.getAttribute(TENANT_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    if (tenant instanceof Enum<?> tenantId) {
      template.header(TENANT_HEADER, tenantId.name());
    }
  }
}
