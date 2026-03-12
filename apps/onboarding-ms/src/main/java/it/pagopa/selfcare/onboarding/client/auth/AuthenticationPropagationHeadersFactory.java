package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;

public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    private static final String AUTHORIZATION = "Authorization";

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        if(incomingHeaders.containsKey(AUTHORIZATION)) {
            List<String> headerValue = incomingHeaders.get(AUTHORIZATION);

            if (headerValue != null) {
                clientOutgoingHeaders.put(AUTHORIZATION, headerValue);
            }

        }
        return clientOutgoingHeaders;
    }
}