package it.pagopa.selfcare.user.client.auth;

import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthenticationPropagationHeadersFactoryTest {

    private final AuthenticationPropagationHeadersFactory factory =
            new AuthenticationPropagationHeadersFactory();

    @Test
    void propagatesAuthorizationAndTenantHeaders() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(AuthenticationPropagationHeadersFactory.AUTHORIZATION_HEADER, List.of("Bearer token"));
        incomingHeaders.put(AuthenticationPropagationHeadersFactory.TENANT_HEADER, List.of("AR"));
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        factory.update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("Bearer token"), outgoingHeaders.get(AuthenticationPropagationHeadersFactory.AUTHORIZATION_HEADER));
        assertEquals(List.of("AR"), outgoingHeaders.get(AuthenticationPropagationHeadersFactory.TENANT_HEADER));
    }

    @Test
    void doesNotAddTenantHeaderWhenMissingFromIncomingRequest() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(AuthenticationPropagationHeadersFactory.AUTHORIZATION_HEADER, List.of("Bearer token"));
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        factory.update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("Bearer token"), outgoingHeaders.get(AuthenticationPropagationHeadersFactory.AUTHORIZATION_HEADER));
        assertNull(outgoingHeaders.get(AuthenticationPropagationHeadersFactory.TENANT_HEADER));
    }
}
