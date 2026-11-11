package it.pagopa.selfcare.onboarding.config;

import io.quarkus.runtime.StartupEvent;
import it.pagopa.selfcare.onboarding.context.TenantJwtSessionAudiences;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates the tenant configuration at startup (fail-closed, SELC-14.9 / SELC-17.6), as in
 * document-ms: {@link TenantRegistry} is a lazy bean that checks itself (including the mandatory
 * {@code contracts} storage) in {@code @PostConstruct}, so without this observer an invalid
 * registry or a tenant without {@code jwt.session.audience} would surface only on the first
 * request or orchestration.
 */
@ApplicationScoped
public class TenantRegistryStartupValidator {

  private static final Logger log = LoggerFactory.getLogger(TenantRegistryStartupValidator.class);

  void onStart(
      @Observes StartupEvent event,
      TenantRegistry tenantRegistry,
      TenantJwtSessionAudiences sessionAudiences) {
    validate(tenantRegistry, sessionAudiences);
  }

  static void validate(TenantRegistry tenantRegistry, TenantJwtSessionAudiences sessionAudiences) {
    log.info("Tenant registry validated, supported tenants: {}", tenantRegistry.supportedTenantIds());
    tenantRegistry
        .supportedTenantIds()
        .forEach(
            tenantId ->
                log.info(
                    "Tenant {} uses JWT session audience {}",
                    tenantId,
                    sessionAudiences.audience(tenantId)));
  }
}
