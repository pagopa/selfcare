package it.pagopa.selfcare.onboarding.integrationTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import it.pagopa.selfcare.cucumber.utils.TestDataProvider;
import it.pagopa.selfcare.cucumber.utils.TestJwtGenerator;
import it.pagopa.selfcare.security.JWTCallerPrincipalFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IntegrationFixtureTest {

    private static final Path RESOURCES = Path.of("src/test/resources");

    @ParameterizedTest
    @ValueSource(strings = {"test-onboarding-ms.env", "test-product-ms.env"})
    void tenantResourcesAndLegacySigningKeyMatchTheSdkFixtures(String filename) throws Exception {
        Map<String, String> environment = Files.readAllLines(RESOURCES.resolve(filename)).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.split("=", 2))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
        var registry = new ObjectMapper().readTree(environment.get("TENANT_REGISTRY_JSON"));
        for (String tenant : new String[] {"AR", "PNPG"}) {
            var config = registry.get(tenant);
            assertNotNull(config, tenant);
            assertFalse(config.has("jwt"), "Fixtures use one legacy key; synthetic tenant kids would reject jwt_test_kid");
            assertTrue(environment.get(config.at("/mongo/connectionStringEnvVar").asText()).startsWith("mongodb://mongo-db:"));
            for (var storage : config.get("storages")) {
                assertTrue(environment.containsKey(storage.at("/authentication/connectionStringEnvVar").asText()));
            }
        }
        String publicKey = environment.get(filename.equals("test-product-ms.env") ? "JWT_PUBLIC_KEY" : "JWT-PUBLIC-KEY");
        JWTCallerPrincipalFactory verifier = new JWTCallerPrincipalFactory(publicKey, null);
        TestJwtGenerator generator = new TestJwtGenerator();
        for (var account : new TestDataProvider().getTestData().getJwtData()) {
            assertNotNull(verifier.parse(generator.generateToken(account), new JWTAuthContextInfo()));
        }
    }

    @Test
    void productCountsMatchTheActiveCatalogWithoutAnEnabledFilter() throws Exception {
        var products = new ObjectMapper().readTree(RESOURCES.resolve("blobStorageInit/products.json").toFile());
        int active = 0;
        int roots = 0;
        int admin = 0;
        for (var product : products) {
            if ("ACTIVE".equals(product.path("status").asText())) {
                active++;
                if (!product.hasNonNull("parentId")) {
                    roots++;
                    if (!product.at("/userContractMappings/DEFAULT/contractTemplatePath").isMissingNode()
                            && !product.at("/userContractMappings/DEFAULT/contractTemplatePath").isNull()) {
                        admin++;
                    }
                }
            }
        }
        assertEquals(14, active);
        assertEquals(11, roots);
        assertEquals(5, admin);
    }

    @Test
    void iamFixturesMatchTheLegacyProductScopedPermissionLookup() throws Exception {
        var expectations = new ObjectMapper().readTree(RESOURCES.resolve("mock/iam-api.json").toFile());
        assertEquals(7, expectations.size());
        var guard = expectations.get(0);
        assertEquals(10, guard.path("priority").asInt());
        assertEquals(".*", guard.at("/httpRequest/queryStringParameters/institutionId/0").asText());
        assertEquals(400, guard.at("/httpResponse/statusCode").asInt());
        int permissions = 0;
        for (var expectation : expectations) {
            if (expectation.has("priority")) {
                continue;
            }
            var request = expectation.get("httpRequest");
            assertTrue(request.path("path").asText().contains("/permissions/Selc:"));
            assertEquals("AR", request.at("/headers/X-Tenant-Id/0").asText());
            assertEquals("prod-(io|pagopa|interop)", request.at("/queryStringParameters/productId/0").asText());
            permissions++;
        }
        assertEquals(6, permissions);
    }

    @Test
    void partyProcessInstitutionListsUseTheContractEnvelope() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var expectations = mapper.readTree(RESOURCES.resolve("mock/pdnd-interop-uservice-party-process.json").toFile());
        int matched = 0;
        for (var expectation : expectations) {
            if ("/pdnd-interop-uservice-party-process/institutions".equals(expectation.at("/httpRequest/path").asText())) {
                assertTrue(mapper.readTree(expectation.at("/httpResponse/body").asText()).path("institutions").isArray());
                matched++;
            }
        }
        assertTrue(matched > 0, "The contract fixture must not silently disappear");
    }
}
