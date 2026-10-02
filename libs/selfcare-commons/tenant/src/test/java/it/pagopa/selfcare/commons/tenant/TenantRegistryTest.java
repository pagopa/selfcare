package it.pagopa.selfcare.commons.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TenantRegistryTest {

    private static final String REGISTRY_JSON = """
            {
              "AR": {
                "mongo": {
                  "account": "cosmos-ar",
                  "database": "groups-ar",
                  "connectionStringEnvVar": "MONGO_AR"
                },
                "jwt": { "publicKeyEnvVar": "JWT_AR" },
                "storages": {
                  "documents": {
                    "account": "storage-ar",
                    "container": "documents",
                    "pathPrefix": "groups",
                    "authentication": {
                      "type": "CONNECTION_STRING",
                      "connectionStringEnvVar": "BLOB_AR"
                    }
                  }
                }
              }
            }
            """;

    @Test
    void resolvesTenantResourcesAndNormalizesIdentifiers() {
        MockEnvironment environment = configuredEnvironment();
        TenantRegistry registry =
                new TenantRegistry(new ObjectMapper(), environment, REGISTRY_JSON, "ar", "documents");

        registry.initialize();

        assertEquals("groups-ar", registry.resolve(" ar ").mongo().database());
        assertEquals("mongodb://localhost:27017", registry.mongoConnectionString("AR").orElseThrow());
        assertEquals("public-key", registry.jwtPublicKey("AR").orElseThrow());
        assertEquals("documents", registry.storage("AR", " DOCUMENTS ").container());
    }

    @Test
    void failsClosedWhenMongoSecretIsMissing() {
        MockEnvironment environment = configuredEnvironment();
        environment.setProperty("MONGO_AR", "");
        TenantRegistry registry =
                new TenantRegistry(new ObjectMapper(), environment, REGISTRY_JSON, "AR", "documents");

        assertThrows(IllegalStateException.class, registry::initialize);
    }

    @Test
    void rejectsUnknownTenantAndStorage() {
        TenantRegistry registry =
                new TenantRegistry(new ObjectMapper(), configuredEnvironment(), REGISTRY_JSON, "AR", "");
        registry.initialize();

        assertThrows(UnknownTenantException.class, () -> registry.resolve("PNPG"));
        assertThrows(UnknownStorageException.class, () -> registry.storage("AR", "contracts"));
    }

    @Test
    void resolvesGlobalOneIdentityAndUserRegistryCredentialsPerTenant() {
        String registryJson = """
                {
                  "AR": {
                    "mongo": {
                      "account": "cosmos-ar",
                      "database": "groups-ar",
                      "connectionStringEnvVar": "MONGO_AR"
                    },
                    "oneIdentity": {
                      "clientIdEnvVar": "ONE_IDENTITY_CLIENT_ID_AR",
                      "clientSecretEnvVar": "ONE_IDENTITY_CLIENT_SECRET_AR"
                    },
                    "userRegistry": {
                      "apiKeyEnvVar": "USER_REGISTRY_API_KEY_AR"
                    }
                  }
                }
                """;
        MockEnvironment environment = configuredEnvironment()
                .withProperty("ONE_IDENTITY_CLIENT_ID_AR", "client-id")
                .withProperty("ONE_IDENTITY_CLIENT_SECRET_AR", "client-secret")
                .withProperty("USER_REGISTRY_API_KEY_AR", "registry-key");
        TenantRegistry registry =
                new TenantRegistry(new ObjectMapper(), environment, registryJson, "AR", "");

        registry.initialize();

        TenantDefinition.OneIdentityCredentials oneIdentity =
                registry.oneIdentityCredentials("AR").orElseThrow();
        assertEquals("client-id", oneIdentity.clientId());
        assertEquals("client-secret", oneIdentity.clientSecret());
        assertEquals("OneIdentityCredentials[REDACTED]", oneIdentity.toString());
        assertEquals("registry-key", registry.userRegistryApiKey("AR"));
        assertEquals(
                "UserRegistryCredentials[REDACTED]",
                registry.userRegistryCredentials("AR").orElseThrow().toString());
    }

    @Test
    void failsStartupWhenConfiguredCredentialSecretIsMissing() {
        String registryJson = """
                {
                  "AR": {
                    "mongo": {
                      "account": "cosmos-ar",
                      "database": "groups-ar",
                      "connectionStringEnvVar": "MONGO_AR"
                    },
                    "userRegistry": { "apiKeyEnvVar": "USER_REGISTRY_API_KEY_AR" }
                  }
                }
                """;
        TenantRegistry registry =
                new TenantRegistry(
                        new ObjectMapper(), configuredEnvironment(), registryJson, "AR", "");

        assertThrows(IllegalStateException.class, registry::initialize);
    }

    @Test
    void failsStartupWhenMandatoryCredentialMappingIsAbsent() {
        TenantRegistry registry =
                new TenantRegistry(
                        new ObjectMapper(),
                        configuredEnvironment(),
                        REGISTRY_JSON,
                        "AR",
                        "",
                        "",
                        "AR");

        assertThrows(IllegalStateException.class, registry::initialize);
    }

    @Test
    void rejectsInvalidCredentialEnvironmentVariableReferences() {
        String registryJson = """
                {
                  "AR": {
                    "mongo": {
                      "account": "cosmos-ar",
                      "database": "groups-ar",
                      "connectionStringEnvVar": "MONGO_AR"
                    },
                    "userRegistry": { "apiKeyEnvVar": "${USER_REGISTRY_API_KEY_AR}" }
                  }
                }
                """;
        TenantRegistry registry =
                new TenantRegistry(
                        new ObjectMapper(),
                        configuredEnvironment(),
                        registryJson,
                        "AR",
                        "");

        assertThrows(IllegalStateException.class, registry::initialize);
    }

    private static MockEnvironment configuredEnvironment() {
        return new MockEnvironment()
                .withProperty("MONGO_AR", "mongodb://localhost:27017")
                .withProperty("JWT_AR", "public-key")
                .withProperty(
                        "BLOB_AR",
                        "DefaultEndpointsProtocol=https;AccountName=test;"
                                + "AccountKey=dGVzdA==;EndpointSuffix=core.windows.net");
    }
}
