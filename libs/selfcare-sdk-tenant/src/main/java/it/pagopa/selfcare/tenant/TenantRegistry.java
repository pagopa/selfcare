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

  @ConfigProperty(name = "tenant.storage.mandatory-keys", defaultValue = "")
  String mandatoryStorageKeys;

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
              validateStorages(tenantId, definition);
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
   * Resolves the raw JWT verification key material (PEM or JWK/JWKS JSON) configured for {@code
   * tenantId}, read from the environment variable named by that tenant's {@code
   * jwt.publicKeyEnvVar}. Returns empty when the tenant has no JWT configuration at all, letting
   * callers fall back to a legacy, non-tenant-scoped verification key.
   */
  public TenantDefinition.StorageDefinition storage(String tenantId, String logicalStorageKey) {
    String normalizedTenantId = normalize(tenantId);
    String normalizedKey = TenantDefinition.normalizeStorageKey(logicalStorageKey);
    TenantDefinition.StorageDefinition storage = resolve(normalizedTenantId).storages().get(normalizedKey);
    if (storage == null) {
      throw new UnknownStorageException(normalizedTenantId, normalizedKey);
    }
    return storage;
  }

  public Optional<String> storageConnectionString(String tenantId, String logicalStorageKey) {
    TenantDefinition.StorageAuthentication authentication = storage(tenantId, logicalStorageKey).authentication();
    if (authentication == null || isBlank(authentication.connectionStringEnvVar())) {
      return Optional.empty();
    }
    return ConfigProvider.getConfig()
        .getOptionalValue(authentication.connectionStringEnvVar(), String.class)
        .map(TenantRegistry::sanitizeConnectionString)
        .filter(value -> !value.isBlank());
  }

  public Optional<String> storageManagedIdentityClientId(String tenantId, String logicalStorageKey) {
    TenantDefinition.StorageAuthentication authentication = storage(tenantId, logicalStorageKey).authentication();
    if (authentication == null || isBlank(authentication.managedIdentityClientIdEnvVar())) {
      return Optional.empty();
    }
    return ConfigProvider.getConfig()
        .getOptionalValue(authentication.managedIdentityClientIdEnvVar(), String.class)
        .filter(value -> !value.isBlank());
  }

  public Set<String> mandatoryStorageKeys() {
    if (mandatoryStorageKeys == null || mandatoryStorageKeys.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(mandatoryStorageKeys.split(","))
        .filter(value -> !value.isBlank())
        .map(TenantDefinition::normalizeStorageKey)
        .collect(Collectors.toUnmodifiableSet());
  }

  public Optional<String> jwtPublicKey(String tenantId) {
    TenantDefinition.JwtDefinition jwt = resolve(tenantId).jwt();
    if (jwt == null || isBlank(jwt.publicKeyEnvVar())) {
      return Optional.empty();
    }
    return ConfigProvider.getConfig()
        .getOptionalValue(jwt.publicKeyEnvVar(), String.class)
        .filter(value -> !value.isBlank());
  }

  /**
   * Cosmos connection strings stored in XML/HTML contexts often encode {@code &} as {@code &amp;},
   * which the Mongo driver rejects as the option {@code amp}.
   */
  static String sanitizeConnectionString(String value) {
    return value.replace("&amp;", "&").trim();
  }

  private void validateStorages(String tenantId, TenantDefinition definition) {
    definition.storages().forEach((logicalKey, storage) -> validateStorageDefinition(tenantId, logicalKey, storage));
    mandatoryStorageKeys()
        .forEach(
            logicalKey -> {
              if (!definition.storages().containsKey(logicalKey)) {
                throw new IllegalStateException(
                    "Missing mandatory storage '" + logicalKey + "' for tenant " + tenantId);
              }
            });
  }

  private void validateStorageDefinition(
      String tenantId, String logicalKey, TenantDefinition.StorageDefinition storage) {
    if (storage == null
        || isBlank(storage.account())
        || isBlank(storage.container())
        || storage.authentication() == null
        || storage.authentication().type() == null) {
      throw new IllegalStateException(
          "Incomplete storage configuration for tenant " + tenantId + " and key " + logicalKey);
    }
    TenantDefinition.StorageAuthentication authentication = storage.authentication();
    boolean hasConnectionString = !isBlank(authentication.connectionStringEnvVar());
    boolean hasManagedIdentityClientId = !isBlank(authentication.managedIdentityClientIdEnvVar());
    switch (authentication.type()) {
      case CONNECTION_STRING -> {
        if (!hasConnectionString || hasManagedIdentityClientId) {
          throw new IllegalStateException(
              "Invalid CONNECTION_STRING storage authentication for tenant "
                  + tenantId
                  + " and key "
                  + logicalKey);
        }
        storageConnectionString(tenantId, logicalKey)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Missing storage connection string environment variable "
                            + authentication.connectionStringEnvVar()
                            + " for tenant "
                            + tenantId
                            + " and key "
                            + logicalKey));
      }
      case MANAGED_IDENTITY -> {
        if (hasConnectionString) {
          throw new IllegalStateException(
              "Invalid MANAGED_IDENTITY storage authentication for tenant "
                  + tenantId
                  + " and key "
                  + logicalKey);
        }
        if (hasManagedIdentityClientId) {
          storageManagedIdentityClientId(tenantId, logicalKey)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "Missing managed identity client id environment variable "
                              + authentication.managedIdentityClientIdEnvVar()
                              + " for tenant "
                              + tenantId
                              + " and key "
                              + logicalKey));
        }
      }
    }
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
