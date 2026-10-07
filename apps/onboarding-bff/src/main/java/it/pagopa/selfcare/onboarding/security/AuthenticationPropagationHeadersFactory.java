package it.pagopa.selfcare.onboarding.security;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Forwards to the downstream services the credentials of the caller, like the Spring BFF does: the
 * {@code Authorization} bearer token and, when not blank, the first value of the {@code X-Tenant-Id}
 * header. Nothing else of the incoming request is forwarded.
 */
public class AuthenticationPropagationHeadersFactory implements ClientHeadersFactory {

    static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
                                                 MultivaluedMap<String, String> clientOutgoingHeaders) {
        forwardFirstValue(incomingHeaders, clientOutgoingHeaders, HttpHeaders.AUTHORIZATION);
        forwardFirstValue(incomingHeaders, clientOutgoingHeaders, TENANT_HEADER);
        return clientOutgoingHeaders;
    }

    private static void forwardFirstValue(MultivaluedMap<String, String> incomingHeaders,
                                          MultivaluedMap<String, String> outgoingHeaders,
                                          String name) {
        String value = incomingHeaders == null ? null : incomingHeaders.getFirst(name);
        if (value != null && !value.isBlank()) {
            outgoingHeaders.putSingle(name, value);
        }
    }
}
