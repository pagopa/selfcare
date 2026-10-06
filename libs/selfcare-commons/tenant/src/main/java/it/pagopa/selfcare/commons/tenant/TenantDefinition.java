package it.pagopa.selfcare.commons.tenant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TenantDefinition(
        @JsonProperty("mongo") MongoDefinition mongo,
        @JsonProperty("jwt") JwtDefinition jwt,
        @JsonProperty("storages") Map<String, StorageDefinition> storages,
        @JsonProperty("oneIdentity") OneIdentityDefinition oneIdentity,
        @JsonProperty("userRegistry") UserRegistryDefinition userRegistry) {

    public TenantDefinition(
            MongoDefinition mongo, JwtDefinition jwt, Map<String, StorageDefinition> storages) {
        this(mongo, jwt, storages, null, null);
    }

    public TenantDefinition {
        storages = normalizeStorages(storages);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OneIdentityDefinition(
            @JsonProperty("clientIdEnvVar") String clientIdEnvVar,
            @JsonProperty("clientSecretEnvVar") String clientSecretEnvVar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserRegistryDefinition(@JsonProperty("apiKeyEnvVar") String apiKeyEnvVar) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MongoDefinition(
            @JsonProperty("account") String account,
            @JsonProperty("database") String database,
            @JsonProperty("connectionStringEnvVar") String connectionStringEnvVar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JwtDefinition(@JsonProperty("publicKeyEnvVar") String publicKeyEnvVar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StorageDefinition(
            @JsonProperty("account") String account,
            @JsonProperty("container") String container,
            @JsonProperty("pathPrefix") String pathPrefix,
            @JsonProperty("authentication") StorageAuthentication authentication) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StorageAuthentication(
            @JsonProperty("type") StorageAuthenticationType type,
            @JsonProperty("managedIdentityClientIdEnvVar") String managedIdentityClientIdEnvVar,
            @JsonProperty("connectionStringEnvVar") String connectionStringEnvVar) {
    }

    static String normalizeStorageKey(String logicalKey) {
        if (logicalKey == null || logicalKey.isBlank()) {
            throw new IllegalArgumentException("Storage logical key is required");
        }
        return logicalKey.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, StorageDefinition> normalizeStorages(
            Map<String, StorageDefinition> storages) {
        if (storages == null || storages.isEmpty()) {
            return Map.of();
        }
        Map<String, StorageDefinition> normalized = new LinkedHashMap<>();
        storages.forEach((key, value) -> {
            String normalizedKey = normalizeStorageKey(key);
            if (normalized.put(normalizedKey, value) != null) {
                throw new IllegalArgumentException(
                        "Duplicate storage logical key: " + normalizedKey);
            }
        });
        return Map.copyOf(normalized);
    }
}
