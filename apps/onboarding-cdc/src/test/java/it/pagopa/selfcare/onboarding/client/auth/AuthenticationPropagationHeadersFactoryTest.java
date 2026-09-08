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
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(TenantContext.TENANT_HEADER, List.of("AR"));

        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("AR"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
    }

    @Test
    void propagatesTenantFromContext() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        try (TenantContext.Scope ignored = TenantContext.open("AR")) {
            authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        }

        assertEquals(List.of("AR"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
    }

    @Test
    void propagatesDefaultTenantWhenMissing() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("PNPG"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
    }
}
