package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;

public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    private static final String AUTHORIZATION = "Authorization";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        if(incomingHeaders.containsKey(AUTHORIZATION)) {
            List<String> headerValue = incomingHeaders.get(AUTHORIZATION);

            if (headerValue != null) {
                clientOutgoingHeaders.put(AUTHORIZATION, headerValue);
            }

        }

        if (incomingHeaders.containsKey(TENANT_HEADER)) {
            List<String> headerValue = incomingHeaders.get(TENANT_HEADER);

            if (headerValue != null) {
                clientOutgoingHeaders.put(TENANT_HEADER, headerValue);
            }
        }

        return clientOutgoingHeaders;
    }
}