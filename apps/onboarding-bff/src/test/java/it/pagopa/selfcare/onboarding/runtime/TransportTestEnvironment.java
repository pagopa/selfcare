package it.pagopa.selfcare.onboarding.runtime;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Environment of the transport tests: like {@link RuntimeTestEnvironment}, but every downstream
 * service is the {@link RawTransportStub}, which can break connections at a precise point.
 */
public class TransportTestEnvironment implements QuarkusTestResourceLifecycleManager {

    private RawTransportStub stub;

    @Override
    public Map<String, String> start() {
        stub = new RawTransportStub();
        Map<String, String> config = new HashMap<>();
        config.put("JWT_TOKEN_PUBLIC_KEY", RuntimeJwt.publicKeyPem());
        config.put("rest-client.ms-onboarding.base-url", stub.url("ms-onboarding"));
        config.put("rest-client.ms-user.base-url", stub.url("ms-user"));
        config.put("rest-client.ms-user-institution.base-url", stub.url("ms-user"));
        config.put("rest-client.ms-product.base-url", stub.url("ms-product"));
        config.put("rest-client.ms-core.base-url", stub.url("ms-core"));
        config.put("rest-client.ms-document.base-url", stub.url("ms-document"));
        config.put("rest-client.ms-iam.base-url", stub.url("ms-iam"));
        config.put("rest-client.party-process.base-url", stub.url("party-process"));
        config.put("rest-client.party-registry-proxy.base-url", stub.url("party-registry-proxy"));
        config.put("rest-client.user-registry.base-url", stub.url("user-registry"));
        config.put("rest-client.onboarding-functions.base-url", stub.url("onboarding-fn"));
        config.put("rest-client.user-registry.api-key", RuntimeTestEnvironment.USER_REGISTRY_API_KEY);
        config.put("ONBOARDING-FUNCTIONS-API-KEY", RuntimeTestEnvironment.ONBOARDING_FUNCTIONS_API_KEY);
        return config;
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(stub,
                new TestInjector.AnnotatedAndMatchesType(InjectStub.class, RawTransportStub.class));
    }

    @Override
    public void stop() {
        if (stub != null) {
            stub.close();
        }
    }
}
