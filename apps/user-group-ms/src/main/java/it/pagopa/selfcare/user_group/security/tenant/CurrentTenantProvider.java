package it.pagopa.selfcare.user_group.security.tenant;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Exposes the tenant already validated for the current request to data-access code.
 *
 * <p>The value comes from {@link TenantValidationFilter}'s request attribute, never from the raw
 * header. Code running outside an active request has no bound {@link RequestAttributes}, so it
 * receives {@link Optional#empty()} and keeps the pre-multitenant behaviour during migration.
 */
@Component
public class CurrentTenantProvider {

  public Optional<String> currentTenantId() {
    RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return Optional.empty();
    }
    Object tenant =
        attributes.getAttribute(
            TenantConstants.TENANT_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
    return tenant instanceof TenantId tenantId ? Optional.of(tenantId.name()) : Optional.empty();
  }
}
