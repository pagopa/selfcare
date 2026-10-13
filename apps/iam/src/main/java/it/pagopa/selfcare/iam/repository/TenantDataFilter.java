package it.pagopa.selfcare.iam.repository;

import it.pagopa.selfcare.iam.model.ProductRoles;
import it.pagopa.selfcare.tenant.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Single point where the tenant of the current request ({@link TenantContext}) is turned into
 * userClaims filters.
 *
 * <p>The user identity is shared by every tenant; the tenant is carried by each embedded product
 * role ({@code productRoles.tenantId}). Until the backfill is verified ({@code
 * selfcare.tenant.strict-data-isolation=false}), legacy product roles without {@code tenantId} are
 * still visible.
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

  /**
   * Matches the product roles of the current tenant.
   *
   * @param prefix path of the product role ({@code productRoles} after an unwind, empty inside an
   *     {@code $elemMatch})
   */
  public Document productRoleFilter(String prefix) {
    String field = prefix == null || prefix.isEmpty() ? TENANT_ID : prefix + "." + TENANT_ID;
    String tenantId = currentTenant();
    if (strictDataIsolation) {
      return new Document(field, tenantId);
    }
    return new Document(
        "$or", Arrays.asList(new Document(field, tenantId), new Document(field, null)));
  }

  public boolean matches(ProductRoles productRole) {
    String tenantId = currentTenant();
    if (productRole.getTenantId() == null) {
      return !strictDataIsolation;
    }
    return tenantId.equals(productRole.getTenantId());
  }
}
