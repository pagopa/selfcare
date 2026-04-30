package it.pagopa.selfcare.user.client.auth;

import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;

public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        if(incomingHeaders.containsKey("Authorization")) {
            List<String> headerValue = incomingHeaders.get("Authorization");

            if (headerValue != null) {
                clientOutgoingHeaders.put("Authorization", headerValue);
            }

        };
        return clientOutgoingHeaders;
    }
}