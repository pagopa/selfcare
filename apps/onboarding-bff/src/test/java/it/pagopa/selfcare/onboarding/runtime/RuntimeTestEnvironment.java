package it.pagopa.selfcare.onboarding.runtime;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import it.pagopa.selfcare.onboarding.parity.DownstreamStub;
import java.util.HashMap;
import java.util.Map;

/**
 * Environment of the runtime tests: the public key and the settings the infrastructure injects
 * (by their environment names), with every downstream service pointing to a controlled stub.
 */
public class RuntimeTestEnvironment implements QuarkusTestResourceLifecycleManager {

    public static final String USER_REGISTRY_API_KEY = "runtime-user-registry-key";
    public static final String ONBOARDING_FUNCTIONS_API_KEY = "runtime-functions-key";
    public static final int IAM_READ_TIMEOUT_MS = 1500;

    private DownstreamStub stub;

    @Override
    public Map<String, String> start() {
        stub = new DownstreamStub();
        Map<String, String> config = new HashMap<>();
        config.put("JWT_TOKEN_PUBLIC_KEY", RuntimeJwt.publicKeyPem());

        config.put("rest-client.ms-onboarding.base-url", stub.url(DownstreamStub.MS_ONBOARDING));
        config.put("rest-client.ms-user.base-url", stub.url(DownstreamStub.MS_USER));
        config.put("rest-client.ms-user-institution.base-url", stub.url(DownstreamStub.MS_USER));
        config.put("rest-client.ms-product.base-url", stub.url(DownstreamStub.MS_PRODUCT));
        config.put("rest-client.ms-core.base-url", stub.url(DownstreamStub.MS_CORE));
        config.put("rest-client.ms-document.base-url", stub.url(DownstreamStub.MS_DOCUMENT));
        config.put("rest-client.ms-iam.base-url", stub.url(DownstreamStub.MS_IAM));
        config.put("rest-client.party-process.base-url", stub.url(DownstreamStub.PARTY_PROCESS));
        config.put("rest-client.party-registry-proxy.base-url", stub.url(DownstreamStub.PARTY_REGISTRY_PROXY));
        config.put("rest-client.user-registry.base-url", stub.url(DownstreamStub.USER_REGISTRY));
        config.put("rest-client.onboarding-functions.base-url", stub.url(DownstreamStub.ONBOARDING_FN));
        config.put("rest-client.user-registry.api-key", USER_REGISTRY_API_KEY);

        config.put("ONBOARDING-FUNCTIONS-API-KEY", ONBOARDING_FUNCTIONS_API_KEY);
        config.put("REST_CLIENT_CONNECT_TIMEOUT", "60000");
        config.put("REST_CLIENT_READ_TIMEOUT", "60000");
        config.put("quarkus.rest-client.iam_json.read-timeout", String.valueOf(IAM_READ_TIMEOUT_MS));
        return config;
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(stub,
                new TestInjector.AnnotatedAndMatchesType(InjectStub.class, DownstreamStub.class));
    }

    @Override
    public void stop() {
        if (stub != null) {
            stub.close();
        }
    }
}
