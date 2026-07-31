package it.pagopa.selfcare.security.tenant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Makes the tenant data isolation registry injectable in Quarkus services (Step_1/EPIC.md
 * sub-task 9).
 *
 * <p>Reads {@code selfcare.tenant.data-isolation}, which Quarkus maps from the
 * {@code SELFCARE_TENANT_DATA_ISOLATION} environment variable set from
 * {@code module.local.config.tenant_data_isolation_json}. The default is an empty object rather
 * than a hardcoded mapping: a service deployed before the variable is wired starts normally and
 * rejects tenant-scoped lookups, instead of silently running against one tenant's resources.
 *
 * <p>Parsing happens once, at bean creation, so a malformed registry stops the service at startup
 * rather than mid-request.
 */
@ApplicationScoped
public class TenantDataIsolationRegistryProducer {

  @ConfigProperty(name = "selfcare.tenant.data-isolation", defaultValue = "{}")
  String rawRegistry;

  @Produces
  @ApplicationScoped
  public TenantDataIsolationRegistry tenantDataIsolationRegistry() {
    return TenantDataIsolationRegistry.fromJson(rawRegistry);
  }
}
