package it.pagopa.selfcare.onboarding.context;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Single point where documents without {@code tenantId} (created before the tenant was introduced)
 * are handled, like {@code TenantDataFilter} in iam and {@code DocumentRepository} in document-ms.
 *
 * <p>Until the backfill is verified ({@code SELFCARE_TENANT_STRICT_DATA_ISOLATION=false}) a missing
 * {@code tenantId} is attributed to the tenant of the current {@link TenantContext}; with strict
 * mode on it is rejected.
 */
@ApplicationScoped
public class TenantDataIsolation {

  public static final String TENANT_ID = "tenantId";

  private final boolean strictDataIsolation;

  public TenantDataIsolation(
      @ConfigProperty(name = "selfcare.tenant.strict-data-isolation", defaultValue = "false")
          boolean strictDataIsolation) {
    this.strictDataIsolation = strictDataIsolation;
  }

  public boolean isStrict() {
    return strictDataIsolation;
  }

  /** Mongo filter matching the documents of the current tenant. */
  public Document tenantFilter() {
    String tenantId = TenantContext.requiredTenant();
    if (strictDataIsolation) {
      return new Document(TENANT_ID, tenantId);
    }
    return new Document(TENANT_ID, new Document("$in", Arrays.asList(tenantId, null)));
  }

  /**
   * Canonical tenant owning a document: its {@code tenantId} when present, otherwise the current
   * tenant if strict mode is off.
   *
   * @throws IllegalArgumentException if the tenant is unsupported, or missing with strict mode on
   *     or without a current tenant
   */
  public String ownerTenant(String documentTenantId) {
    if (documentTenantId == null || documentTenantId.isBlank()) {
      if (strictDataIsolation) {
        throw new IllegalArgumentException("Missing tenantId with strict tenant data isolation");
      }
      return TenantContext.requiredTenant();
    }
    return TenantContext.resolve(documentTenantId);
  }
}
