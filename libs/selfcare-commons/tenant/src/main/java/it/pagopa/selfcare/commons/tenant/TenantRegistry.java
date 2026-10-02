package it.pagopa.selfcare.commons.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

public class TenantRegistry {

    private static final Pattern ENV_VAR_NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final String registryJson;
    private final String supportedTenants;
    private final String mandatoryStorageKeys;
    private final String mandatoryOneIdentityTenants;
    private final String mandatoryUserRegistryTenants;
    private Map<String, TenantDefinition> tenants = Collections.emptyMap();

    public TenantRegistry(
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${tenant.registry.json:{}}") String registryJson,
            @Value("${tenant.supported-tenants:*}") String supportedTenants,
            @Value("${tenant.storage.mandatory-keys:}") String mandatoryStorageKeys) {
        this(objectMapper, environment, registryJson, supportedTenants, mandatoryStorageKeys, "", "");
    }

    public TenantRegistry(
            ObjectMapper objectMapper,
            Environment environment,
            String registryJson,
            String supportedTenants,
            String mandatoryStorageKeys,
            String mandatoryOneIdentityTenants,
            String mandatoryUserRegistryTenants) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.registryJson = registryJson;
        this.supportedTenants = supportedTenants;
        this.mandatoryStorageKeys = mandatoryStorageKeys;
        this.mandatoryOneIdentityTenants = mandatoryOneIdentityTenants;
        this.mandatoryUserRegistryTenants = mandatoryUserRegistryTenants;
    }

    @PostConstruct
    public void initialize() {
        try {
            Map<String, TenantDefinition> parsed =
                    objectMapper.readValue(registryJson, new TypeReference<>() {});
            tenants = parsed.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            entry -> normalizeTenantId(entry.getKey()), Map.Entry::getValue));
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid tenant registry configuration", exception);
        }

        supportedTenantIds().forEach(tenantId -> {
            TenantDefinition definition = tenants.get(tenantId);
            if (definition == null || definition.mongo() == null) {
                throw new IllegalStateException("Missing Mongo configuration for tenant " + tenantId);
            }
            validateMongo(tenantId, definition.mongo());
            validateJwt(tenantId, definition.jwt());
            validateCredentials(tenantId, definition);
            validateStorages(tenantId, definition);
        });
        validateMandatoryCredentials(supportedTenantIds(), mandatoryOneIdentityTenants, true);
        validateMandatoryCredentials(supportedTenantIds(), mandatoryUserRegistryTenants, false);
    }

    public boolean isConfigured() {
        return !tenants.isEmpty();
    }

    public TenantDefinition resolve(String tenantId) {
        String normalized = normalizeTenantId(tenantId);
        if (!supportedTenantIds().contains(normalized)) {
            throw new UnknownTenantException(normalized);
        }
        TenantDefinition definition = tenants.get(normalized);
        if (definition == null) {
            throw new UnknownTenantException(normalized);
        }
        return definition;
    }

    public String normalizeAndValidate(String tenantId) {
        String normalized = normalizeTenantId(tenantId);
        resolve(normalized);
        return normalized;
    }

    public Set<String> supportedTenantIds() {
        if (supportedTenants == null
                || supportedTenants.isBlank()
                || "*".equals(supportedTenants.trim())) {
            return tenants.keySet();
        }
        return Arrays.stream(supportedTenants.split(","))
                .map(TenantRegistry::normalizeTenantId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Optional<String> mongoConnectionString(String tenantId) {
        return property(resolve(tenantId).mongo().connectionStringEnvVar());
    }

    public Optional<String> jwtPublicKey(String tenantId) {
        TenantDefinition.JwtDefinition jwt = resolve(tenantId).jwt();
        return jwt == null ? Optional.empty() : property(jwt.publicKeyEnvVar());
    }

    public Optional<TenantDefinition.OneIdentityCredentials> oneIdentityCredentials(
            String tenantId) {
        TenantDefinition.OneIdentityDefinition definition = resolve(tenantId).oneIdentity();
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.of(
                new TenantDefinition.OneIdentityCredentials(
                        requiredSecret(
                                definition.clientIdEnvVar(),
                                tenantId,
                                "OneIdentity client ID"),
                        requiredSecret(
                                definition.clientSecretEnvVar(),
                                tenantId,
                                "OneIdentity client secret")));
    }

    public Optional<TenantDefinition.UserRegistryCredentials> userRegistryCredentials(
            String tenantId) {
        TenantDefinition.UserRegistryDefinition definition = resolve(tenantId).userRegistry();
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.of(
                new TenantDefinition.UserRegistryCredentials(
                        requiredSecret(
                                definition.apiKeyEnvVar(), tenantId, "User Registry API key")));
    }

    public String userRegistryApiKey(String tenantId) {
        return userRegistryCredentials(tenantId)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "User Registry is not configured for tenant "
                                                + normalizeTenantId(tenantId)))
                .apiKey();
    }

    public TenantDefinition.StorageDefinition storage(String tenantId, String logicalKey) {
        String normalizedTenant = normalizeAndValidate(tenantId);
        String normalizedKey = TenantDefinition.normalizeStorageKey(logicalKey);
        TenantDefinition.StorageDefinition storage =
                resolve(normalizedTenant).storages().get(normalizedKey);
        if (storage == null) {
            throw new UnknownStorageException(normalizedTenant, normalizedKey);
        }
        return storage;
    }

    public Optional<String> storageConnectionString(String tenantId, String logicalKey) {
        TenantDefinition.StorageAuthentication authentication =
                storage(tenantId, logicalKey).authentication();
        return property(authentication.connectionStringEnvVar());
    }

    public Optional<String> storageManagedIdentityClientId(String tenantId, String logicalKey) {
        TenantDefinition.StorageAuthentication authentication =
                storage(tenantId, logicalKey).authentication();
        return property(authentication.managedIdentityClientIdEnvVar());
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

    private Optional<String> property(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(environment.getProperty(name))
                .map(String::trim)
                .filter(value -> !value.isBlank());
    }

    private String requiredSecret(String envVarName, String tenantId, String credentialName) {
        validateEnvVarName(envVarName, tenantId, credentialName);
        return property(envVarName)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Missing "
                                                + credentialName
                                                + " environment variable "
                                                + envVarName
                                                + " for tenant "
                                                + normalizeTenantId(tenantId)));
    }

    private void validateCredentials(String tenantId, TenantDefinition definition) {
        if (definition.oneIdentity() != null) {
            requiredSecret(
                    definition.oneIdentity().clientIdEnvVar(),
                    tenantId,
                    "OneIdentity client ID");
            requiredSecret(
                    definition.oneIdentity().clientSecretEnvVar(),
                    tenantId,
                    "OneIdentity client secret");
        }
        if (definition.userRegistry() != null) {
            requiredSecret(
                    definition.userRegistry().apiKeyEnvVar(), tenantId, "User Registry API key");
        }
    }

    private void validateMandatoryCredentials(
            Set<String> supported, String configuredTenants, boolean oneIdentity) {
        if (configuredTenants == null || configuredTenants.isBlank()) {
            return;
        }
        List<String> mandatoryTenants =
                Arrays.stream(configuredTenants.split(","))
                        .filter(value -> !value.isBlank())
                        .map(TenantRegistry::normalizeTenantId)
                        .toList();
        for (String tenantId : mandatoryTenants) {
            if (!supported.contains(tenantId)) {
                throw new IllegalStateException(
                        "Mandatory credential tenant is not supported: " + tenantId);
            }
            TenantDefinition definition = tenants.get(tenantId);
            if (definition == null
                    || (oneIdentity
                            ? definition.oneIdentity() == null
                            : definition.userRegistry() == null)) {
                throw new IllegalStateException(
                        "Missing mandatory "
                                + (oneIdentity ? "OneIdentity" : "User Registry")
                                + " configuration for tenant "
                                + tenantId);
            }
        }
    }

    private void validateEnvVarName(String name, String tenantId, String resourceName) {
        if (name == null || !ENV_VAR_NAME.matcher(name).matches()) {
            throw new IllegalStateException(
                    "Invalid " + resourceName + " environment variable reference for tenant "
                            + normalizeTenantId(tenantId));
        }
    }

    private void validateMongo(String tenantId, TenantDefinition.MongoDefinition mongo) {
        if (isBlank(mongo.account()) || isBlank(mongo.database())
                || isBlank(mongo.connectionStringEnvVar())) {
            throw new IllegalStateException("Incomplete Mongo configuration for tenant " + tenantId);
        }
        validateEnvVarName(mongo.connectionStringEnvVar(), tenantId, "Mongo connection string");
        mongoConnectionString(tenantId).orElseThrow(() -> new IllegalStateException(
                "Missing Mongo connection string environment variable "
                        + mongo.connectionStringEnvVar() + " for tenant " + tenantId));
    }

    private void validateJwt(String tenantId, TenantDefinition.JwtDefinition jwt) {
        if (jwt != null) {
            validateEnvVarName(jwt.publicKeyEnvVar(), tenantId, "JWT public key");
            jwtPublicKey(tenantId).orElseThrow(() -> new IllegalStateException(
                    "Missing JWT public key environment variable "
                            + jwt.publicKeyEnvVar() + " for tenant " + tenantId));
        }
    }

    private void validateStorages(String tenantId, TenantDefinition definition) {
        definition.storages().forEach(
                (logicalKey, storage) -> validateStorage(tenantId, logicalKey, storage));
        mandatoryStorageKeys().forEach(logicalKey -> {
            if (!definition.storages().containsKey(logicalKey)) {
                throw new IllegalStateException(
                        "Missing mandatory storage '" + logicalKey + "' for tenant " + tenantId);
            }
        });
    }

    private void validateStorage(
            String tenantId, String logicalKey, TenantDefinition.StorageDefinition storage) {
        if (storage == null || isBlank(storage.account()) || isBlank(storage.container())
                || storage.authentication() == null || storage.authentication().type() == null) {
            throw new IllegalStateException(
                    "Incomplete storage configuration for tenant " + tenantId
                            + " and key " + logicalKey);
        }
        TenantDefinition.StorageAuthentication authentication = storage.authentication();
        boolean connectionString = !isBlank(authentication.connectionStringEnvVar());
        boolean clientId = !isBlank(authentication.managedIdentityClientIdEnvVar());
        if (authentication.type() == StorageAuthenticationType.CONNECTION_STRING) {
            if (!connectionString || clientId) {
                throw new IllegalStateException(
                        "Invalid CONNECTION_STRING storage authentication for tenant "
                                + tenantId + " and key " + logicalKey);
            }
            validateEnvVarName(
                    authentication.connectionStringEnvVar(), tenantId, "Storage connection string");
            storageConnectionString(tenantId, logicalKey).orElseThrow(
                    () -> new IllegalStateException(
                            "Missing storage connection string environment variable "
                                    + authentication.connectionStringEnvVar()
                                    + " for tenant " + tenantId + " and key " + logicalKey));
        } else if (connectionString) {
            throw new IllegalStateException(
                    "Invalid MANAGED_IDENTITY storage authentication for tenant "
                            + tenantId + " and key " + logicalKey);
        } else if (clientId) {
            validateEnvVarName(
                    authentication.managedIdentityClientIdEnvVar(),
                    tenantId,
                    "Managed identity client ID");
            storageManagedIdentityClientId(tenantId, logicalKey).orElseThrow(
                    () -> new IllegalStateException(
                            "Missing managed identity client id environment variable "
                                    + authentication.managedIdentityClientIdEnvVar()
                                    + " for tenant " + tenantId + " and key " + logicalKey));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant id is required");
        }
        return tenantId.trim().toUpperCase(Locale.ROOT);
    }
}
