package it.pagopa.selfcare.auth.conf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TenantRegistry {

  public static final String ONE_IDENTITY = "ONE_IDENTITY";

  @Inject ObjectMapper objectMapper;
  @Inject it.pagopa.selfcare.tenant.TenantRegistry resourceRegistry;

  @ConfigProperty(name = "tenant.registry.json")
  String tenantRegistryJson;

  private Map<String, TenantDefinition> tenants;

  @PostConstruct
  void initialize() {
    try {
      tenants =
          Map.copyOf(
              objectMapper.readValue(
                  tenantRegistryJson, new TypeReference<Map<String, TenantDefinition>>() {}));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Invalid tenant registry configuration", e);
    }

  }

  public Tenant resolveEnabledTenant(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new InvalidRequestException("X-Tenant-Id header is required");
    }

    TenantDefinition tenantDefinition = tenants.get(tenantId);
    if (tenantDefinition == null) {
      throw new InvalidRequestException("Unknown tenant");
    }
    if (!tenantDefinition.authEnabled()) {
      throw new ForbiddenException("Tenant is not enabled for auth");
    }

    return new Tenant(tenantId, tenantDefinition);
  }

  public OneIdentityCredentials oneIdentityCredentials(String tenantId) {
    Tenant tenant = resolveEnabledTenant(tenantId);
    if (!ONE_IDENTITY.equals(tenant.definition().authenticationProvider())) {
      throw new ForbiddenException("OneIdentity is not enabled for tenant");
    }
    return resourceRegistry
        .oneIdentityCredentials(tenantId)
        .map(credentials -> new OneIdentityCredentials(credentials.clientId(), credentials.clientSecret()))
        .orElseThrow(() -> new IllegalStateException("OneIdentity is not configured for tenant " + tenantId));
  }

  public Collection<Tenant> enabledAuthenticationTenants() {
    return tenants.entrySet().stream()
        .filter(entry -> entry.getValue().authEnabled())
        .map(entry -> new Tenant(entry.getKey(), entry.getValue()))
        .toList();
  }

  public record Tenant(String id, TenantDefinition definition) {}

  public record OneIdentityCredentials(String clientId, String clientSecret) {
    @Override
    public String toString() {
      return "OneIdentityCredentials[REDACTED]";
    }
  }
}
