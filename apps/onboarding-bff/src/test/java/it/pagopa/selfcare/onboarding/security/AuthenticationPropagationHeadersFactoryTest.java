package it.pagopa.selfcare.onboarding.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Spring BFF forwards the caller token and the first X-Tenant-Id value to every downstream service, nothing else. */
class AuthenticationPropagationHeadersFactoryTest {

    private static final String BEARER = "Bearer caller-token";

    private final AuthenticationPropagationHeadersFactory factory = new AuthenticationPropagationHeadersFactory();

    @Test
    void forwardsAuthorizationAndTenant() {
        MultivaluedMap<String, String> outgoing = update(incoming(BEARER, "PNPG"), new MultivaluedHashMap<>());

        assertEquals(BEARER, outgoing.getFirst("Authorization"));
        assertEquals("PNPG", outgoing.getFirst("X-Tenant-Id"));
    }

    @Test
    void forwardsOnlyTheFirstValueOfDuplicatedHeaders() {
        MultivaluedMap<String, String> incoming = incoming(BEARER, "PNPG");
        incoming.add("X-Tenant-Id", "AR");
        incoming.add("Authorization", "Bearer other-token");

        MultivaluedMap<String, String> outgoing = update(incoming, new MultivaluedHashMap<>());

        assertEquals(List.of("PNPG"), outgoing.get("X-Tenant-Id"));
        assertEquals(List.of(BEARER), outgoing.get("Authorization"));
    }

    @Test
    void doesNotForwardAbsentOrBlankHeaders() {
        MultivaluedMap<String, String> outgoing = update(incoming(null, " "), new MultivaluedHashMap<>());

        assertNull(outgoing.get("Authorization"));
        assertNull(outgoing.get("X-Tenant-Id"));
    }

    @Test
    void tenantIsOptionalLikeForPagopaTokens() {
        MultivaluedMap<String, String> outgoing = update(incoming(BEARER, null), new MultivaluedHashMap<>());

        assertEquals(BEARER, outgoing.getFirst("Authorization"));
        assertNull(outgoing.get("X-Tenant-Id"));
    }

    @Test
    void forwardsTheTenantAsReceived() {
        MultivaluedMap<String, String> outgoing = update(incoming(BEARER, "zz"), new MultivaluedHashMap<>());

        assertEquals("zz", outgoing.getFirst("X-Tenant-Id"));
    }

    @Test
    void doesNotForwardOtherIncomingHeaders() {
        MultivaluedMap<String, String> incoming = incoming(BEARER, "PNPG");
        incoming.add("X-Correlation-Id", "corr");
        incoming.add("X-Client-Ip", "10.0.0.1");
        incoming.add("x-api-key", "caller-key");
        incoming.add("x-functions-key", "caller-key");
        incoming.add("Cookie", "a=b");

        MultivaluedMap<String, String> outgoing = update(incoming, new MultivaluedHashMap<>());

        assertEquals(2, outgoing.size());
        assertTrue(outgoing.containsKey("Authorization") && outgoing.containsKey("X-Tenant-Id"));
    }

    @Test
    void keepsTheHeadersSetByTheClient() {
        MultivaluedMap<String, String> clientHeaders = new MultivaluedHashMap<>();
        clientHeaders.putSingle("x-api-key", "client-key");

        MultivaluedMap<String, String> outgoing = update(incoming(BEARER, "PNPG"), clientHeaders);

        assertSame(clientHeaders, outgoing);
        assertEquals("client-key", outgoing.getFirst("x-api-key"));
        assertEquals(BEARER, outgoing.getFirst("Authorization"));
    }

    @Test
    void withoutIncomingHeadersTheClientHeadersAreUntouched() {
        MultivaluedMap<String, String> clientHeaders = new MultivaluedHashMap<>();
        clientHeaders.putSingle("x-api-key", "client-key");

        MultivaluedMap<String, String> outgoing = factory.update(null, clientHeaders);

        assertEquals(1, outgoing.size());
        assertEquals("client-key", outgoing.getFirst("x-api-key"));
    }

    private MultivaluedMap<String, String> update(MultivaluedMap<String, String> incoming,
                                                  MultivaluedMap<String, String> outgoing) {
        return factory.update(incoming, outgoing);
    }

    private static MultivaluedMap<String, String> incoming(String authorization, String tenant) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (authorization != null) {
            headers.add("Authorization", authorization);
        }
        if (tenant != null) {
            headers.add("X-Tenant-Id", tenant);
        }
        return headers;
    }
}
