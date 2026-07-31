package it.pagopa.selfcare.external_api.security.tenant;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Propagates the tenant onto outgoing service-to-service calls (Step_0 SELC-5).
 *
 * <p>Forwarding the JWT alone is not enough once downstream services enforce tenant consistency:
 * their {@code TenantValidationFilter} rejects with 400 any request that carries a JWT but no
 * {@code X-Tenant-Id} header. Every internal call made by a Feign client configured with
 * {@code AuthorizationHeaderInterceptor} would therefore fail, so this interceptor is paired with
 * it on the same clients.
 *
 * <p>The tenant is read from the request attribute set by {@link TenantValidationFilter} - the
 * <b>validated</b> value, already reconciled against the JWT claim - never echoed from the raw
 * incoming header, which would forward unvalidated input if the filter were ever bypassed for a
 * path.
 *
 * <p>Outside an active request (scheduled work, async threads without the request context) no
 * header is added, mirroring how the Authorization header degrades: a guessed tenant would be
 * worse than none.
 *
 * <p>Deliberately not a {@code @Component}: registering it globally would attach it to every Feign
 * client, including the ones calling third parties (user-registry, PagoPA back-office, national
 * registries), leaking the internal tenant identifier outside the platform. It is imported
 * per-client instead, exactly like {@code AuthorizationHeaderInterceptor}.
 */
public class TenantHeaderInterceptor implements RequestInterceptor {

  @Override
  public void apply(RequestTemplate template) {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return;
    }
    Object tenant =
        attributes.getAttribute(
            TenantConstants.TENANT_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    if (tenant instanceof TenantId tenantId) {
      template.header(TenantConstants.TENANT_HEADER, tenantId.name());
    }
  }
}
