package it.pagopa.selfcare.iam.repository;

import it.pagopa.selfcare.iam.model.ProductRoles;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Single point where the tenant of the current request ({@link TenantContext}) is turned into
 * userClaims filters.
 *
 * <p>Documents are scoped by the top-level {@code tenantId}. Until the backfill is verified ({@code
 * selfcare.tenant.strict-data-isolation=false}), legacy documents without {@code tenantId} are
 * still visible. Embedded product roles without {@code tenantId} belong to the document tenant.
 */
@ApplicationScoped
public class TenantDataFilter {

  public static final String TENANT_ID = "tenantId";

  private final TenantContext tenantContext;
  private final boolean strictDataIsolation;

  @Inject
  public TenantDataFilter(
      TenantContext tenantContext,
      @ConfigProperty(name = "selfcare.tenant.strict-data-isolation", defaultValue = "false")
          boolean strictDataIsolation) {
    this.tenantContext = tenantContext;
    this.strictDataIsolation = strictDataIsolation;
  }

  public String currentTenant() {
    return tenantContext.requiredTenantId();
  }

  /** Restricts {@code filter} to the userClaims documents of the current tenant. */
  public Document scoped(Document filter) {
    return new Document("$and", List.of(filter, documentFilter()));
  }

  /** Matches the userClaims documents of the current tenant. */
  public Document documentFilter() {
    String tenantId = currentTenant();
    if (strictDataIsolation) {
      return new Document(TENANT_ID, tenantId);
    }
    return new Document(
        "$or", Arrays.asList(new Document(TENANT_ID, tenantId), new Document(TENANT_ID, null)));
  }

  /**
   * Matches the product roles of the current tenant.
   *
   * @param prefix path of the product role ({@code productRoles} after an unwind, empty inside an
   *     {@code $elemMatch})
   */
  public Document productRoleFilter(String prefix) {
    String field = prefix == null || prefix.isEmpty() ? TENANT_ID : prefix + "." + TENANT_ID;
    return new Document(
        "$or", Arrays.asList(new Document(field, currentTenant()), new Document(field, null)));
  }

  public boolean matches(ProductRoles productRole) {
    return productRole.getTenantId() == null || currentTenant().equals(productRole.getTenantId());
  }
}
