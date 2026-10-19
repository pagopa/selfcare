package it.pagopa.selfcare.onboarding.client.auth;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class AuthenticationPropagationHeadersFactoryTest {

    @Inject
    AuthenticationPropagationHeadersFactory authenticationPropagationHeadersFactory;

    @Test
    void propagatesIncomingTenantHeader() {
        // given
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(TenantContext.TENANT_HEADER, List.of("AR"));

        // when
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);

        // then
        assertEquals(List.of("AR"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
        assertEquals(List.of("Bearer test-technical-jwt"), outgoingHeaders.get("Authorization"));
    }

    @Test
    void propagatesTenantFromContext() {
        // given
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        // when
        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        }

        // then
        assertEquals(List.of("AR"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
        assertEquals(List.of("Bearer test-technical-jwt"), outgoingHeaders.get("Authorization"));
    }

    @Test
    void propagatesDefaultTenantWhenMissing() {
        // given
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        // when
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);

        // then
        assertEquals(List.of("PNPG"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
        assertEquals(List.of("Bearer test-technical-jwt"), outgoingHeaders.get("Authorization"));
    }
}
