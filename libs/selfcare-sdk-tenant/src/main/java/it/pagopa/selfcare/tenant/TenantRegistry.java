package it.pagopa.selfcare.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TenantRegistry {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @ConfigProperty(name = "tenant.registry.json", defaultValue = "{}")
  String tenantRegistryJson;

  @ConfigProperty(name = "tenant.supported-tenants", defaultValue = "*")
  String supportedTenants;

  private Map<String, TenantDefinition> tenants = Collections.emptyMap();

  @PostConstruct
  void initialize() {
    try {
      Map<String, TenantDefinition> parsed =
          objectMapper.readValue(tenantRegistryJson, new TypeReference<>() {});
      tenants =
          parsed.entrySet().stream()
              .collect(
                  Collectors.toUnmodifiableMap(
                      entry -> normalize(entry.getKey()), Map.Entry::getValue));
    } catch (JsonProcessingException | IllegalArgumentException exception) {
      throw new IllegalStateException("Invalid tenant registry configuration", exception);
    }

    supportedTenantIds()
        .forEach(
            tenantId -> {
              TenantDefinition definition = tenants.get(tenantId);
              if (definition == null || definition.mongo() == null) {
                throw new IllegalStateException(
                    "Missing Mongo configuration for tenant " + tenantId);
              }
              validateMongoDefinition(tenantId, definition.mongo());
            });
  }

  public TenantDefinition resolve(String tenantId) {
    String normalizedTenantId = normalize(tenantId);
    if (!supportedTenantIds().contains(normalizedTenantId)) {
      throw new UnknownTenantException(normalizedTenantId);
    }
    TenantDefinition definition = tenants.get(normalizedTenantId);
    if (definition == null || definition.mongo() == null) {
      throw new UnknownTenantException(normalizedTenantId);
    }
    return definition;
  }

  public String normalizeTenantId(String tenantId) {
    return normalize(tenantId);
  }

  public Set<String> supportedTenantIds() {
    if (supportedTenants == null
        || supportedTenants.isBlank()
        || "*".equals(supportedTenants.trim())) {
      return tenants.keySet();
    }
    return Arrays.stream(supportedTenants.split(","))
        .map(TenantRegistry::normalize)
        .collect(Collectors.toUnmodifiableSet());
  }

  public Map<String, TenantDefinition> definitions() {
    return tenants;
  }

  public Optional<String> connectionString(String tenantId) {
    TenantDefinition.MongoDefinition mongo = resolve(tenantId).mongo();
    return ConfigProvider.getConfig()
        .getOptionalValue(mongo.connectionStringEnvVar(), String.class)
        .map(TenantRegistry::sanitizeConnectionString)
        .filter(value -> !value.isBlank());
  }

  /**
   * Cosmos connection strings stored in XML/HTML contexts often encode {@code &} as
   * {@code &amp;}, which the Mongo driver rejects as the option {@code amp}.
   */
  static String sanitizeConnectionString(String value) {
    return value.replace("&amp;", "&").trim();
  }

  private void validateMongoDefinition(String tenantId, TenantDefinition.MongoDefinition mongo) {
    if (isBlank(mongo.account())
        || isBlank(mongo.database())
        || isBlank(mongo.connectionStringEnvVar())) {
      throw new IllegalStateException("Incomplete Mongo configuration for tenant " + tenantId);
    }
    connectionString(tenantId)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Missing Mongo connection string environment variable "
                        + mongo.connectionStringEnvVar()
                        + " for tenant "
                        + tenantId));
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String normalize(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("Tenant id is required");
    }
    return tenantId.trim().toUpperCase(Locale.ROOT);
  }
}
