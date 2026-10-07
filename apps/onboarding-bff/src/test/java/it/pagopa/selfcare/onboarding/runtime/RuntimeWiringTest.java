package it.pagopa.selfcare.onboarding.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub;
import it.pagopa.selfcare.onboarding.security.DownstreamApiKeyFilter;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

/** How the runtime is wired: security SDK beans, credentials of the downstream clients and settings from the infrastructure environment. */
@QuarkusTest
@TestProfile(RuntimeTestProfile.class)
class RuntimeWiringTest {

    private static final String RESTEASY_CLIENT_PREFIX = "quarkus.rest-client.";

    @InjectStub
    DownstreamStub stub;

    @Inject
    DownstreamApiKeyFilter downstreamApiKeyFilter;

    @Test
    void jwtCallerPrincipalFactory_isTheOnlyOneAndBelongsToTheBff() throws ClassNotFoundException {
        Object factory = ClientProxy.unwrap(Arc.container().instance(JWTCallerPrincipalFactory.class).get());

        assertEquals("it.pagopa.selfcare.onboarding.security.BffJwtCallerPrincipalFactory", factory.getClass().getName());
        assertTrue(Arc.container().select(Class.forName("it.pagopa.selfcare.security.JWTCallerPrincipalFactory"))
                .isUnsatisfied());
    }

    @Test
    void securitySdkIdentityAugmentor_isNotActive() throws ClassNotFoundException {
        Class<?> augmentor = Class.forName("it.pagopa.selfcare.security.JWTSecurityIdentityAugmentor");

        assertTrue(Arc.container().select(augmentor).isUnsatisfied());
    }

    @Test
    void downstreamClients_receiveTheConfiguredGlobalApiKey() {
        MultivaluedMap<String, String> incoming = new MultivaluedHashMap<>();
        incoming.add("Authorization", "Bearer caller-token");
        incoming.add("X-Tenant-Id", "AR");

        MultivaluedMap<String, Object> outgoing = new MultivaluedHashMap<>();
        incoming.forEach((name, values) -> outgoing.put(name, new java.util.ArrayList<>(values)));
        ClientRequestContext request = mock(ClientRequestContext.class);
        when(request.getHeaders()).thenReturn(outgoing);
        downstreamApiKeyFilter.filter(request);

        assertEquals("Bearer caller-token", outgoing.getFirst("Authorization"));
        assertEquals("AR", outgoing.getFirst("X-Tenant-Id"));
        assertEquals(RuntimeTestEnvironment.USER_REGISTRY_API_KEY, outgoing.getFirst("x-api-key"));
        assertTrue(!outgoing.containsKey("x-functions-key"));
    }

    @Test
    void restClientTimeouts_keepEffectiveSpringDefaultsAndTheExplicitTestOverride() {
        Config config = ConfigProvider.getConfig();

        assertEquals(10000L, timeout(config, "iam_json", "connect-timeout"));
        assertEquals(RuntimeTestEnvironment.IAM_READ_TIMEOUT_MS, timeout(config, "iam_json", "read-timeout"));
        for (String client : new String[] {"institution_json", "onboarding_json", "user_json", "party_process",
                "party_registry_proxy", "user_registry_json"}) {
            assertEquals(10000L, timeout(config, client, "connect-timeout"), client);
            assertEquals(60000L, timeout(config, client, "read-timeout"), client);
        }
    }

    @Test
    void aggregatesAndInstitutionApiClients_useTheSameEffectiveSpringDefaults() {
        Config config = ConfigProvider.getConfig();

        for (String api : new String[] {"AggregatesControllerApi", "InstitutionControllerApi"}) {
            String client = "\"org.openapi.quarkus.onboarding_json.api." + api + "\"";
            assertEquals(10000L, timeout(config, client, "connect-timeout"), api);
            assertEquals(60000L, timeout(config, client, "read-timeout"), api);
        }
    }

    @Test
    void restClients_areBoundToTheConfiguredServices() {
        Config config = ConfigProvider.getConfig();

        assertEquals(stub.url(DownstreamStub.MS_IAM), config.getValue(RESTEASY_CLIENT_PREFIX + "iam_json.url", String.class));
        assertEquals(stub.url(DownstreamStub.MS_ONBOARDING), config.getValue(RESTEASY_CLIENT_PREFIX + "onboarding_json.url", String.class));
        assertEquals(stub.url(DownstreamStub.ONBOARDING_FN), config.getValue(RESTEASY_CLIENT_PREFIX + "onboarding_functions_json.url", String.class));
        assertEquals(stub.url(DownstreamStub.MS_USER),
                config.getValue(RESTEASY_CLIENT_PREFIX + "\"org.openapi.quarkus.user_json.api.InstitutionControllerApi\".url", String.class));
    }

    private static long timeout(Config config, String client, String property) {
        return config.getValue(RESTEASY_CLIENT_PREFIX + client + "." + property, Long.class);
    }
}
