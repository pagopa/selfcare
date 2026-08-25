package it.pagopa.selfcare.iam.conf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.iam.exception.InvalidRequestException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Holds the known tenants for this microservice, resolved from the {@code tenant.registry.json}
 * configuration property. Unlike {@code apps/auth}, iam does not run any authentication flow, so
 * this registry is only used to validate that an {@code X-Tenant-Id} header (when tenant
 * enforcement is enabled) refers to a known tenant.
 */
@ApplicationScoped
public class TenantRegistry {

  @Inject ObjectMapper objectMapper;

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

  public Tenant resolveTenant(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new InvalidRequestException("X-Tenant-Id header is required");
    }

    TenantDefinition tenantDefinition = tenants.get(tenantId);
    if (tenantDefinition == null) {
      throw new InvalidRequestException("Unknown tenant");
    }

    return new Tenant(tenantId, tenantDefinition);
  }

  public record Tenant(String id, TenantDefinition definition) {}
}
