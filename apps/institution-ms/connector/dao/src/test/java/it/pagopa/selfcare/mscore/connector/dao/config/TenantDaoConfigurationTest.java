package it.pagopa.selfcare.mscore.connector.dao.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import it.pagopa.selfcare.commons.tenant.UnknownTenantException;
import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.mock.env.MockEnvironment;

class TenantDaoConfigurationTest {

    @Test
    void defaultRegistryPreservesExistingMongoSettingsAndUsesTenantAliases() throws IOException {
        MockEnvironment environment = environmentWithDaoProperties()
                .withProperty(
                        "MONGODB_CONNECTION_URI",
                        "mongodb://mongo.example:27017/?replicaSet=rs0&appName=custom")
                .withProperty("MONGODB_APPNAME", "institution")
                .withProperty("MONGODB_NAME_SELC_MSCORE", "existing-institution-db")
                .withProperty("jwt.signingKey", "test-public-key")
                .withProperty("INSTITUTION_PRODUCTS_STORAGE_CONNECTION_STRING", "products-connection")
                .withProperty(
                        "INSTITUTION_MAIL_TEMPLATES_STORAGE_CONNECTION_STRING",
                        "mail-templates-connection");

        TenantRegistry registry = tenantRegistry(environment, environment.getProperty("tenant.registry.json"));

        assertEquals("AR", environment.getProperty("tenant.default"));
        assertEquals("AR", registry.normalizeAndValidate("ar"));
        assertEquals("existing-institution-db", registry.resolve("AR").mongo().database());
        assertEquals(
                "mongodb://mongo.example:27017/?replicaSet=rs0&appName=custom&appname=institution",
                registry.mongoConnectionString("AR").orElseThrow());
        assertEquals("institution", environment.getProperty("tenant.mongodb.application-name"));
        assertEquals("test-public-key", registry.jwtPublicKey("AR").orElseThrow());
        assertEquals("true", environment.getProperty("tenant.strict-data-isolation"));
    }

    @Test
    void tenantMongoApplicationNameDefaultsToExistingInstitutionName() throws IOException {
        MockEnvironment environment = environmentWithDaoProperties();

        assertEquals("selcMsCore", environment.getProperty("tenant.mongodb.application-name"));
    }

    @Test
    void registrySupportsExplicitTenantAllowlistAndSeparateConnections() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MONGO_AR", "mongodb://ar:27017")
                .withProperty("MONGO_PNPG", "mongodb://pnpg:27017");
        String registryJson = """
                {
                  "AR": {"mongo": {"account": "ar", "database": "institution-ar", "connectionStringEnvVar": "MONGO_AR"}},
                  "PNPG": {"mongo": {"account": "pnpg", "database": "institution-pnpg", "connectionStringEnvVar": "MONGO_PNPG"}}
                }
                """;

        TenantRegistry registry = tenantRegistry(environment, registryJson, "AR,PNPG");

        assertEquals("institution-ar", registry.resolve("AR").mongo().database());
        assertEquals("mongodb://pnpg:27017", registry.mongoConnectionString("PNPG").orElseThrow());
        assertThrows(UnknownTenantException.class, () -> registry.normalizeAndValidate("DEV"));
    }

    @Test
    void invalidOrMissingTenantBindingsFailWithoutSecretFallback() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MONGO_AR", "mongodb://ar:27017");

        assertThrows(
                IllegalStateException.class,
                () -> tenantRegistry(
                        environment,
                        """
                        {"AR":{"mongo":{"account":"ar","database":"db","connectionStringEnvVar":"MONGO.AR"}}}
                        """,
                        "AR"));
        assertThrows(
                IllegalStateException.class,
                () -> tenantRegistry(environment, "{}", "AR"));
        assertThrows(
                IllegalStateException.class,
                () -> tenantRegistry(
                        new MockEnvironment(),
                        """
                        {"AR":{"mongo":{"account":"ar","database":"db","connectionStringEnvVar":"MONGO_AR"}}}
                        """,
                        "AR"));
    }

    @Test
    void mandatoryBlobBindingsAndCredentialsAreValidatedAtStartup() {
        MockEnvironment environment = new MockEnvironment()
                        .withProperty("MONGO_AR", "mongodb://ar:27017");

        assertThrows(
                        IllegalStateException.class,
                        () -> tenantRegistry(
                                environment,
                                """
                                {"AR":{"mongo":{"account":"ar","database":"db","connectionStringEnvVar":"MONGO_AR"}}}
                                """,
                                "AR",
                                "products,mail-templates"));
        assertThrows(
                        IllegalStateException.class,
                        () -> tenantRegistry(
                                environment,
                                """
                                {
                                  "AR": {
                                    "mongo": {"account":"ar","database":"db","connectionStringEnvVar":"MONGO_AR"},
                                    "storages": {
                                      "products": {
                                        "account":"products",
                                        "container":"products",
                                        "authentication": {
                                          "type":"CONNECTION_STRING",
                                          "connectionStringEnvVar":"MISSING_BLOB_CONNECTION"
                                        }
                                      }
                                    }
                                  }
                                }
                                """,
                                "AR",
                                "products"));
    }

    private MockEnvironment environmentWithDaoProperties() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("config/dao-config.properties"));
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(
                new PropertiesPropertySource("dao-config", properties));
        return environment;
    }

    private TenantRegistry tenantRegistry(MockEnvironment environment, String registryJson) {
        return tenantRegistry(environment, registryJson, "AR");
    }

    private TenantRegistry tenantRegistry(
            MockEnvironment environment, String registryJson, String supportedTenants) {
        return tenantRegistry(
                environment,
                registryJson,
                supportedTenants,
                environment.getProperty("tenant.storage.mandatory-keys", ""));
    }

    private TenantRegistry tenantRegistry(
            MockEnvironment environment,
            String registryJson,
            String supportedTenants,
            String mandatoryStorageKeys) {
        TenantRegistry registry = new TenantRegistry(
                new ObjectMapper(), environment, registryJson, supportedTenants, mandatoryStorageKeys);
        registry.initialize();
        return registry;
    }
}
