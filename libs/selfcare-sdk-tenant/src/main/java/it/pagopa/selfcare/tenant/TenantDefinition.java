package it.pagopa.selfcare.tenant;

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
        @JsonProperty("userRegistry") UserRegistryDefinition userRegistry,
        @JsonProperty("signature") SignatureDefinition signature) {

    public TenantDefinition {
        storages = copyStorages(storages);
    }

    public TenantDefinition(MongoDefinition mongo, JwtDefinition jwt) {
        this(mongo, jwt, Map.of());
    }

    public TenantDefinition(MongoDefinition mongo, JwtDefinition jwt, Map<String, StorageDefinition> storages) {
        this(mongo, jwt, storages, null, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OneIdentityDefinition(
            @JsonProperty("clientIdEnvVar") String clientIdEnvVar,
            @JsonProperty("clientSecretEnvVar") String clientSecretEnvVar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserRegistryDefinition(@JsonProperty("apiKeyEnvVar") String apiKeyEnvVar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SignatureDefinition(
            @JsonProperty("source") String source,
            @JsonProperty("signer") String signer,
            @JsonProperty("location") String location,
            @JsonProperty("reason") String reason,
            @JsonProperty("namirial") NamirialSignatureDefinition namirial,
            @JsonProperty("aruba") ArubaSignatureDefinition aruba) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamirialSignatureDefinition(
            @JsonProperty("baseUrlEnvVar") String baseUrlEnvVar,
            @JsonProperty("userEnvVar") String userEnvVar,
            @JsonProperty("passwordEnvVar") String passwordEnvVar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ArubaSignatureDefinition(
            @JsonProperty("baseUrlEnvVar") String baseUrlEnvVar,
            @JsonProperty("typeOtpAuthEnvVar") String typeOtpAuthEnvVar,
            @JsonProperty("otpPwdEnvVar") String otpPwdEnvVar,
            @JsonProperty("userEnvVar") String userEnvVar,
            @JsonProperty("delegatedUserEnvVar") String delegatedUserEnvVar,
            @JsonProperty("delegatedPasswordEnvVar") String delegatedPasswordEnvVar,
            @JsonProperty("delegatedDomainEnvVar") String delegatedDomainEnvVar,
            @JsonProperty("connectTimeoutMsEnvVar") String connectTimeoutMsEnvVar,
            @JsonProperty("requestTimeoutMsEnvVar") String requestTimeoutMsEnvVar) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MongoDefinition(
            @JsonProperty("account") String account,
            @JsonProperty("database") String database,
            @JsonProperty("connectionStringEnvVar") String connectionStringEnvVar) {
    }

    /**
     * Optional per-tenant JWT verification key configuration. When present, {@code
     * publicKeyEnvVar} names the environment variable holding either a raw PEM public key or a
     * JWK/JWKS JSON document for this tenant. Unlike {@link MongoDefinition}, this is not
     * mandatory: apps that don't need per-tenant JWT keys can omit it entirely and rely on the
     * legacy {@code mp.jwt.verify.publickey} property instead.
     */
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

    static String normalizeStorageKey(String logicalStorageKey) {
        if (logicalStorageKey == null || logicalStorageKey.isBlank()) {
            throw new IllegalArgumentException("Storage logical key is required");
        }
        return logicalStorageKey.trim().toLowerCase(Locale.ROOT);
    }

    private static Map<String, StorageDefinition> copyStorages(Map<String, StorageDefinition> storages) {
        if (storages == null || storages.isEmpty()) {
            return Map.of();
        }
        Map<String, StorageDefinition> normalized = new LinkedHashMap<>();
        storages.forEach((key, value) -> {
            String normalizedKey = normalizeStorageKey(key);
            if (normalized.put(normalizedKey, value) != null) {
                throw new IllegalArgumentException("Duplicate storage logical key: " + normalizedKey);
            }
        });
        return Map.copyOf(normalized);
    }
}
