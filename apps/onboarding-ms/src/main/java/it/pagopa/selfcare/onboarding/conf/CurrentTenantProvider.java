package it.pagopa.selfcare.onboarding.conf;

import it.pagopa.selfcare.security.tenant.TenantContext;
import it.pagopa.selfcare.security.tenant.TenantId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;
import java.util.Optional;

/**
 * Exposes the tenant already validated for the current request (Step_0 {@code TenantContext}) to
 * application-scoped beans, without forcing them to handle CDI scope mismatches.
 *
 * <p>{@code TenantContext} is {@code @RequestScoped}, so reading it from code running outside an
 * active request - scheduled jobs, orchestration callbacks, startup tasks - throws {@link
 * ContextNotActiveException}. This provider turns that, and the "request with no tenant resolved"
 * case, into an empty {@link Optional} so callers can decide explicitly what to do.
 */
@ApplicationScoped
public class CurrentTenantProvider {

  @Inject TenantContext tenantContext;

  public Optional<String> currentTenantId() {
    try {
      return Optional.ofNullable(tenantContext.getTenant()).map(TenantId::name);
    } catch (ContextNotActiveException e) {
      return Optional.empty();
    }
  }
}
