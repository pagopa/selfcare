package it.pagopa.selfcare.onboarding.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthenticationPropagationHeadersFactoryTest {

    @Test
    void propagatesAuthorizationAndTenantHeaders() {
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put("Authorization", List.of("Bearer token"));
        incomingHeaders.put("X-Tenant-Id", List.of("AR"));
        MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        new AuthenticationPropagationHeadersFactory().update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("Bearer token"), outgoingHeaders.get("Authorization"));
        assertEquals(List.of("AR"), outgoingHeaders.get("X-Tenant-Id"));
    }

    @Test
    void doesNotAddHeadersWhenTheyAreMissing() {
        MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        new AuthenticationPropagationHeadersFactory().update(new MultivaluedHashMap<>(), outgoingHeaders);

        assertEquals(0, outgoingHeaders.size());
    }
}
