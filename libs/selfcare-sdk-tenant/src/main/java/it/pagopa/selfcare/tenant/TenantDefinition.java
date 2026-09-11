package it.pagopa.selfcare.tenant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TenantDefinition(
        @JsonProperty("mongo") MongoDefinition mongo, @JsonProperty("jwt") JwtDefinition jwt) {

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
}
