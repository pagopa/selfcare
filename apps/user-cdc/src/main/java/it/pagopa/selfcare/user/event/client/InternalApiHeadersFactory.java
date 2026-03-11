package it.pagopa.selfcare.user.event.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

public class InternalApiHeadersFactory implements ClientHeadersFactory {

    private static final String API_KEY_NAME = "Ocp-Apim-Subscription-Key";

    @Inject
    @ConfigProperty(name = "client.internal.api-key")
    private String apiKey;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> multivaluedMap, MultivaluedMap<String, String> multivaluedMap1) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle(API_KEY_NAME, apiKey);
        return headers;
    }

}
