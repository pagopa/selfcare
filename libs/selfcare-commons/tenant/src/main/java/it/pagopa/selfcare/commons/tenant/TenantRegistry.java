package it.pagopa.selfcare.commons.tenant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

public class TenantRegistry {

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final String registryJson;
    private final String supportedTenants;
    private final String mandatoryStorageKeys;
    private Map<String, TenantDefinition> tenants = Collections.emptyMap();

    public TenantRegistry(
            ObjectMapper objectMapper,
            Environment environment,
            @Value("${tenant.registry.json:{}}") String registryJson,
            @Value("${tenant.supported-tenants:*}") String supportedTenants,
            @Value("${tenant.storage.mandatory-keys:}") String mandatoryStorageKeys) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.registryJson = registryJson;
        this.supportedTenants = supportedTenants;
        this.mandatoryStorageKeys = mandatoryStorageKeys;
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
            validateStorages(tenantId, definition);
        });
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

    private void validateMongo(String tenantId, TenantDefinition.MongoDefinition mongo) {
        if (isBlank(mongo.account()) || isBlank(mongo.database())
                || isBlank(mongo.connectionStringEnvVar())) {
            throw new IllegalStateException("Incomplete Mongo configuration for tenant " + tenantId);
        }
        mongoConnectionString(tenantId).orElseThrow(() -> new IllegalStateException(
                "Missing Mongo connection string environment variable "
                        + mongo.connectionStringEnvVar() + " for tenant " + tenantId));
    }

    private void validateJwt(String tenantId, TenantDefinition.JwtDefinition jwt) {
        if (jwt != null && !isBlank(jwt.publicKeyEnvVar())) {
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
