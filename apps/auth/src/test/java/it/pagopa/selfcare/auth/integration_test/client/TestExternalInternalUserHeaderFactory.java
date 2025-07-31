package it.pagopa.selfcare.auth.integration_test.client;

import it.pagopa.selfcare.cucumber.utils.SharedStepData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
@ApplicationScoped
@Slf4j
public class TestExternalInternalUserHeaderFactory implements ClientHeadersFactory {

    private static final String HEADER_NAME = "Ocp-Apim-Subscription-Key";

    @Inject
    SharedStepData sharedStepData;

    @Inject
    @ConfigProperty(name = "internal.user-api.key")
    String apiKey;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incoming,
            MultivaluedMap<String, String> outgoing) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(HEADER_NAME, apiKey);
        headers.putSingle("Authorization", "Bearer "+sharedStepData.getToken());
        return headers;
    }
}