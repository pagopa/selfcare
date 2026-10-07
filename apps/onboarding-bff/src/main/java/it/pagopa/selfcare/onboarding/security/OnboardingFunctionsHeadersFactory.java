package it.pagopa.selfcare.onboarding.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Headers of the calls to the onboarding functions: the caller credentials, as for the other
 * downstream services, plus the Azure Functions key.
 */
@ApplicationScoped
public class OnboardingFunctionsHeadersFactory implements ClientHeadersFactory {

    static final String FUNCTIONS_KEY_HEADER = "x-functions-key";

    private final AuthenticationPropagationHeadersFactory propagation = new AuthenticationPropagationHeadersFactory();

    @ConfigProperty(name = "rest-client.onboarding-functions.api-key")
    String functionsKey;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        MultivaluedMap<String, String> headers = propagation.update(incomingHeaders, clientOutgoingHeaders);
        headers.putSingle(FUNCTIONS_KEY_HEADER, functionsKey);
        return headers;
    }
}
