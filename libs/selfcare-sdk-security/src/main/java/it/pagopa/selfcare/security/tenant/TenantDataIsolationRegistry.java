package it.pagopa.selfcare.security.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves the data-layer resources of an already-validated tenant (Step_1 SELC-8..SELC-11,
 * Step_1/EPIC.md sub-task 9).
 *
 * <p>The registry is the application-side view of {@code local.tenant_data_isolation}
 * ({@code infra/resources/_modules/local-env/locals.tf}), the single source of truth shared with
 * the Step_0 routing registry in the same file. It is delivered to a service as one JSON
 * environment variable ({@code SELFCARE_TENANT_DATA_ISOLATION} →
 * {@code selfcare.tenant.data-isolation}), so a service never carries its own copy of a tenant
 * mapping and adding a tenant or a dimension is a Terraform change, not a code change.
 *
 * <p>Every lookup is fail-closed. A tenant absent from the registry, or a dimension left undecided
 * for it, raises {@link UnresolvedTenantMappingException} rather than returning a default: routing
 * to another tenant's database, storage account, vault or sender domain is precisely the outcome
 * SELC-8.4 / SELC-9.4 / SELC-10.2 / SELC-11.2 forbid.
 *
 * <p>The tenant argument must be the tenant already validated upstream ({@link TenantContext},
 * populated by {@link TenantValidationFilter}); this class never derives a tenant itself
 * (SELC-8.5, SELC-9.5).
 */
public final class TenantDataIsolationRegistry {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final Map<TenantId, TenantDataMapping> mappings;

  private TenantDataIsolationRegistry(Map<TenantId, TenantDataMapping> mappings) {
    this.mappings = mappings;
  }

  /**
   * Builds a registry from the JSON produced by {@code local.tenant_data_isolation_json}.
   *
   * <p>A blank payload yields an empty registry: a service deployed before the env var is wired up
   * still starts, and fails only if and when it actually asks for a mapping. A malformed payload or
   * an unknown tenant key fails here instead, at startup, because it means the registry and the
   * {@link TenantId} enum disagree — which no request-time behaviour can safely resolve.
   */
  public static TenantDataIsolationRegistry fromJson(String json) {
    Map<TenantId, TenantDataMapping> parsed = new EnumMap<>(TenantId.class);
    if (json == null || json.isBlank()) {
      return new TenantDataIsolationRegistry(parsed);
    }

    Map<String, TenantDataMapping> raw;
    try {
      raw = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, TenantDataMapping>>() {});
    } catch (Exception e) {
      throw new IllegalStateException(
          "Invalid tenant data isolation registry: payload is not a tenant -> mapping JSON object", e);
    }

    raw.forEach(
        (tenant, mapping) -> {
          TenantId tenantId =
              TenantId.fromValue(tenant)
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Invalid tenant data isolation registry: unknown tenant '"
                                  + tenant
                                  + "'"));
          parsed.put(tenantId, mapping);
        });
    return new TenantDataIsolationRegistry(parsed);
  }

  /** Whole mapping of a tenant, or {@link UnresolvedTenantMappingException} if it has none. */
  public TenantDataMapping mappingFor(TenantId tenant) {
    if (tenant == null) {
      throw new UnresolvedTenantMappingException(
          "No tenant resolved for this operation; refusing to access tenant-scoped data");
    }
    TenantDataMapping mapping = mappings.get(tenant);
    if (mapping == null) {
      throw new UnresolvedTenantMappingException(
          "Tenant " + tenant + " has no entry in the tenant data isolation registry");
    }
    return mapping;
  }

  /** Cosmos DB account holding the tenant's data (SELC-8.4 database-per-tenant model). */
  public String cosmosAccountName(TenantId tenant) {
    return require(tenant, TenantDataMapping::cosmosAccountName, "cosmos_account_name");
  }

  /** Resource group of {@link #cosmosAccountName(TenantId)}. */
  public String cosmosResourceGroupName(TenantId tenant) {
    return require(tenant, TenantDataMapping::cosmosResourceGroupName, "cosmos_resource_group_name");
  }

  /**
   * Key Vault secret NAME holding the tenant's Cosmos DB connection string. The registry carries
   * the reference; the secret itself never leaves Key Vault (Step_1/SECURITY.md).
   */
  public String cosmosConnectionStringSecretName(TenantId tenant) {
    return require(
        tenant,
        TenantDataMapping::cosmosConnectionStringSecretName,
        "cosmos_connection_string_secret_name");
  }

  /** Tenant infix of the storage account names (SELC-9.4 per-tenant storage account model). */
  public String storageAccountInfix(TenantId tenant) {
    return require(tenant, TenantDataMapping::storageAccountInfix, "storage_account_infix");
  }

  /** Tenant suffix of container names (SELC-9.3 shared account, per-tenant container model). */
  public String storageContainerSuffix(TenantId tenant) {
    return require(tenant, TenantDataMapping::storageContainerSuffix, "storage_container_suffix");
  }

  /**
   * Container a blob operation must use, derived from the service's own base container name and the
   * validated tenant — never from client-supplied input (SELC-9.3).
   */
  public String storageContainer(TenantId tenant, String baseContainerName) {
    if (baseContainerName == null || baseContainerName.isBlank()) {
      throw new UnresolvedTenantMappingException(
          "Cannot derive a tenant container from a blank base container name");
    }
    return baseContainerName + storageContainerSuffix(tenant);
  }

  /** Personal data vault instance/tenant to call for this tenant (SELC-10.2). */
  public String personalDataVaultTenant(TenantId tenant) {
    return require(
        tenant, TenantDataMapping::personalDataVaultTenant, "personal_data_vault_tenant");
  }

  /** Sender domain outbound email must use for this tenant (SELC-11.2). */
  public String emailSenderDomain(TenantId tenant) {
    return require(tenant, TenantDataMapping::emailSenderDomain, "email_sender_domain");
  }

  private String require(
      TenantId tenant, Function<TenantDataMapping, String> dimension, String dimensionName) {
    String value = dimension.apply(mappingFor(tenant));
    if (value == null) {
      throw new UnresolvedTenantMappingException(
          "Tenant " + tenant + " has no " + dimensionName + " configured; refusing to fall back");
    }
    return value;
  }
}
