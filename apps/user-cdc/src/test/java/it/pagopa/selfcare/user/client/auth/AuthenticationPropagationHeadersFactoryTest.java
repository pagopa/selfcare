package it.pagopa.selfcare.user.client.auth;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationPropagationHeadersFactoryTest {

    private final AuthenticationPropagationHeadersFactory factory =
            new AuthenticationPropagationHeadersFactory();

    @Test
    void update_shouldCopyAuthorizationFromIncomingHeaders() {
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put("Authorization", List.of("Bearer token"));
        MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        MultivaluedMap<String, String> result = factory.update(incomingHeaders, outgoingHeaders);

        assertSame(outgoingHeaders, result);
        assertEquals(List.of("Bearer token"), result.get("Authorization"));
    }

    @Test
    void update_shouldKeepExistingOutgoingHeadersWhenCopyingAuthorization() {
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put("Authorization", List.of("Bearer token"));
        MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        outgoingHeaders.put("X-Request-Id", List.of("req-1"));

        MultivaluedMap<String, String> result = factory.update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("Bearer token"), result.get("Authorization"));
        assertEquals(List.of("req-1"), result.get("X-Request-Id"));
    }

    @Test
    void update_shouldNotAddAuthorizationWhenIncomingHeaderIsMissing() {
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        MultivaluedMap<String, String> result = factory.update(incomingHeaders, outgoingHeaders);

        assertSame(outgoingHeaders, result);
        assertFalse(result.containsKey("Authorization"));
    }

    @Test
    void update_shouldNotOverwriteOutgoingWhenAuthorizationValueIsNull() {
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put("Authorization", null);
        MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        outgoingHeaders.put("X-Request-Id", List.of("req-1"));

        MultivaluedMap<String, String> result = factory.update(incomingHeaders, outgoingHeaders);

        assertFalse(result.containsKey("Authorization"));
        assertEquals(List.of("req-1"), result.get("X-Request-Id"));
    }
}
