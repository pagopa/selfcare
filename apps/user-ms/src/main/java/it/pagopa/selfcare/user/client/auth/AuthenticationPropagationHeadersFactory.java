package it.pagopa.selfcare.user.client.auth;

import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import java.util.List;

public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
        copyHeader(incomingHeaders, clientOutgoingHeaders, AUTHORIZATION_HEADER);
        copyHeader(incomingHeaders, clientOutgoingHeaders, TENANT_HEADER);
        return clientOutgoingHeaders;
    }

    private static void copyHeader(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders,
            String headerName) {
        if (incomingHeaders.containsKey(headerName)) {
            List<String> headerValue = incomingHeaders.get(headerName);
            if (headerValue != null) {
                clientOutgoingHeaders.put(headerName, headerValue);
            }
        }
    }
}