package it.pagopa.selfcare.onboarding.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Audience of the JWTs issued by the functions, read from {@code <tenant>.jwt.session.audience} of
 * {@code TENANT_REGISTRY_JSON} (same key used by {@code auth}). The SDK {@link TenantRegistry} only
 * maps {@code jwt.publicKeyEnvVar}, so the session section is parsed here. A tenant without an
 * audience is rejected: there is no fallback to another tenant.
 */
@ApplicationScoped
public class TenantJwtSessionAudiences {

  private final TenantRegistry registry;
  private final Map<String, String> audiences = new HashMap<>();

  @Inject
  public TenantJwtSessionAudiences(
      TenantRegistry registry,
      @ConfigProperty(name = "tenant.registry.json", defaultValue = "{}") String registryJson) {
    this.registry = registry;
    try {
      new ObjectMapper()
          .readTree(registryJson)
          .fields()
          .forEachRemaining(
              tenant -> {
                JsonNode audience = tenant.getValue().path("jwt").path("session").path("audience");
                if (audience.isTextual() && !audience.asText().isBlank()) {
                  audiences.put(registry.normalizeTenantId(tenant.getKey()), audience.asText().trim());
                }
              });
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Invalid tenant registry configuration", exception);
    }
  }

  /**
   * @throws IllegalArgumentException if the tenant is not supported
   * @throws IllegalStateException if the tenant has no {@code jwt.session.audience}
   */
  public String audience(String tenant) {
    String canonicalTenant = TenantContext.resolve(tenant);
    String audience = audiences.get(canonicalTenant);
    if (audience == null) {
      throw new IllegalStateException("No JWT session audience configured for tenant " + canonicalTenant);
    }
    return audience;
  }
}
