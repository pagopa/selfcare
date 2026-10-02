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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TenantRegistry {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final Pattern ENV_VAR_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  @ConfigProperty(name = "tenant.registry.json", defaultValue = "{}")
  String tenantRegistryJson;

  /** Optional resource registry when tenant.registry.json is used for other tenant metadata. */
  @ConfigProperty(name = "tenant.resources.registry.json")
  Optional<String> tenantResourcesRegistryJson = Optional.empty();

  @ConfigProperty(name = "tenant.supported-tenants", defaultValue = "*")
  String supportedTenants;

  /** Disable only for applications that use tenant resources without Mongo. */
  @ConfigProperty(name = "tenant.mongo.mandatory", defaultValue = "true")
  boolean mongoMandatory = true;

  @ConfigProperty(name = "tenant.storage.mandatory-keys")
  Optional<String> mandatoryStorageKeys = Optional.empty();

  /** Comma-separated supported tenant IDs requiring OneIdentity; empty leaves it optional. */
  @ConfigProperty(name = "tenant.one-identity.mandatory-tenants")
  Optional<String> mandatoryOneIdentityTenants = Optional.empty();

  /** Comma-separated supported tenant IDs requiring UserRegistry; empty leaves it optional. */
  @ConfigProperty(name = "tenant.user-registry.mandatory-tenants")
  Optional<String> mandatoryUserRegistryTenants = Optional.empty();

  private Map<String, TenantDefinition> tenants = Collections.emptyMap();

  @PostConstruct
  void initialize() {
    try {
      String resourcesJson = tenantResourcesRegistryJson.orElse("");
      Map<String, TenantDefinition> parsed =
          objectMapper.readValue(
              resourcesJson.isBlank() ? tenantRegistryJson : resourcesJson,
              new TypeReference<>() {});
      tenants =
          parsed.entrySet().stream()
              .collect(
                  Collectors.toUnmodifiableMap(
                      entry -> normalize(entry.getKey()), Map.Entry::getValue));
    } catch (JsonProcessingException | IllegalArgumentException | IllegalStateException exception) {
      throw new IllegalStateException("Invalid tenant registry configuration", exception);
    }

    Set<String> supported = supportedTenantIds();
    validateMandatoryTenants(supported, mandatoryOneIdentityTenants.orElse(""), "OneIdentity");
    validateMandatoryTenants(supported, mandatoryUserRegistryTenants.orElse(""), "UserRegistry");
    supported
        .forEach(
            tenantId -> {
              TenantDefinition definition = tenants.get(tenantId);
              if (definition == null || (mongoMandatory && definition.mongo() == null)) {
                throw new IllegalStateException(
                    "Missing Mongo configuration for tenant " + tenantId);
              }
              if (definition.mongo() != null) {
                validateMongoDefinition(tenantId, definition.mongo());
              }
              validateStorages(tenantId, definition);
              validateCredentialDefinitions(tenantId, definition);
            });
  }

  public TenantDefinition resolve(String tenantId) {
    String normalizedTenantId = normalize(tenantId);
    if (!supportedTenantIds().contains(normalizedTenantId)) {
      throw new UnknownTenantException(normalizedTenantId);
    }
    TenantDefinition definition = tenants.get(normalizedTenantId);
    if (definition == null || (mongoMandatory && definition.mongo() == null)) {
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
    if (mongo == null) {
      throw new IllegalStateException("Mongo is not configured for tenant " + normalize(tenantId));
    }
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
    String configured = mandatoryStorageKeys.orElse("");
    if (configured.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(configured.split(","))
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

  /** Returns empty only when OneIdentity is not configured for this tenant. */
  public Optional<OneIdentityCredentials> oneIdentityCredentials(String tenantId) {
    TenantDefinition.OneIdentityDefinition definition = resolve(tenantId).oneIdentity();
    if (definition == null) {
      return Optional.empty();
    }
    return Optional.of(new OneIdentityCredentials(
        requiredSecret(definition.clientIdEnvVar(), tenantId, "OneIdentity client id"),
        requiredSecret(definition.clientSecretEnvVar(), tenantId, "OneIdentity client secret")));
  }

  /** Returns empty only when UserRegistry is not configured for this tenant. */
  public Optional<UserRegistryCredentials> userRegistryCredentials(String tenantId) {
    TenantDefinition.UserRegistryDefinition definition = resolve(tenantId).userRegistry();
    if (definition == null) {
      return Optional.empty();
    }
    return Optional.of(new UserRegistryCredentials(
        requiredSecret(definition.apiKeyEnvVar(), tenantId, "UserRegistry API key")));
  }

  /** Requires a configured UserRegistry API key; never falls back to a shared key. */
  public String userRegistryApiKey(String tenantId) {
    return userRegistryCredentials(tenantId)
        .orElseThrow(() -> new IllegalStateException(
            "UserRegistry is not configured for tenant " + normalize(tenantId)))
        .apiKey();
  }

  public record OneIdentityCredentials(String clientId, String clientSecret) {
    @Override
    public String toString() {
      return "OneIdentityCredentials[REDACTED]";
    }
  }

  public record UserRegistryCredentials(String apiKey) {
    @Override
    public String toString() {
      return "UserRegistryCredentials[REDACTED]";
    }
  }

  private void validateCredentialDefinitions(String tenantId, TenantDefinition definition) {
    if (definition.oneIdentity() == null) {
      if (configuredFor(mandatoryOneIdentityTenants.orElse(""), tenantId)) {
        throw new IllegalStateException("Missing mandatory OneIdentity configuration for tenant " + tenantId);
      }
    } else {
      oneIdentityCredentials(tenantId);
    }
    if (definition.userRegistry() == null) {
      if (configuredFor(mandatoryUserRegistryTenants.orElse(""), tenantId)) {
        throw new IllegalStateException("Missing mandatory UserRegistry configuration for tenant " + tenantId);
      }
    } else {
      userRegistryCredentials(tenantId);
    }
  }

  private String requiredSecret(String reference, String tenantId, String dimension) {
    if (reference == null || !ENV_VAR_NAME.matcher(reference).matches()) {
      throw new IllegalStateException("Invalid " + dimension + " environment variable reference for tenant " + tenantId);
    }
    return ConfigProvider.getConfig().getOptionalValue(reference, String.class)
        .filter(value -> !value.isBlank())
        .orElseThrow(() -> new IllegalStateException(
            "Missing " + dimension + " environment variable for tenant " + tenantId));
  }

  private void validateMandatoryTenants(Set<String> supported, String configured, String dimension) {
    for (String tenantId : tenantIds(configured)) {
      if (!supported.contains(tenantId)) {
        throw new IllegalStateException("Mandatory " + dimension + " tenant is not supported: " + tenantId);
      }
    }
  }

  private static boolean configuredFor(String configured, String tenantId) {
    return tenantIds(configured).contains(tenantId);
  }

  private static Set<String> tenantIds(String configured) {
    if (configured == null || configured.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(configured.split(",", -1)).map(TenantRegistry::normalize)
        .collect(Collectors.toUnmodifiableSet());
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
