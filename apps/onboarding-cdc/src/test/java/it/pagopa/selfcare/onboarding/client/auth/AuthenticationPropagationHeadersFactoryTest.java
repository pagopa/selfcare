package it.pagopa.selfcare.onboarding.client.auth;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class AuthenticationPropagationHeadersFactoryTest {

    @Inject
    AuthenticationPropagationHeadersFactory authenticationPropagationHeadersFactory;

    @Test
    void propagatesIncomingTenantHeader() {
        // given
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(TenantContext.TENANT_HEADER, List.of("PNPG"));

        // when
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);

        // then
        assertEquals(List.of("PNPG"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
        assertTechnicalAuthorization(outgoingHeaders);
    }

    @Test
    void propagatesTenantFromContext() {
        // given
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        // when
        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        }

        // then
        assertEquals(List.of("PNPG"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
        assertTechnicalAuthorization(outgoingHeaders);
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
        assertTechnicalAuthorization(outgoingHeaders);
    }

    @Test
    void rejectsTechnicalTokenForDifferentTenant() {
        // given
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(TenantContext.TENANT_HEADER, List.of("AR"));

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders));

        // then
        assertEquals(
                "Invalid technical JWT for onboarding-functions: tenant_id does not match X-Tenant-Id",
                exception.getMessage());
    }

    private static void assertTechnicalAuthorization(
            MultivaluedHashMap<String, String> outgoingHeaders) {
        String authorization = outgoingHeaders.getFirst("Authorization");
        assertTrue(authorization.startsWith("Bearer eyJ"));
    }
}
