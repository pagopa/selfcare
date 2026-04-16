package it.pagopa.selfcare.institution.client.auth;

import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;

public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        String bearerToken = System.getenv("JWT_BEARER_TOKEN");
        clientOutgoingHeaders.put("Authorization", List.of("Bearer " + bearerToken));
        return clientOutgoingHeaders;
    }
}