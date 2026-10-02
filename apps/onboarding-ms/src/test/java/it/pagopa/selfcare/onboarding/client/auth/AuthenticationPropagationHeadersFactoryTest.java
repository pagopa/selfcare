package it.pagopa.selfcare.onboarding.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.TenantRegistry;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticationPropagationHeadersFactoryTest {

    private TenantContext tenantContext;
    private TenantRegistry tenantRegistry;
    private AuthenticationPropagationHeadersFactory factory;

    @BeforeEach
    void setUp() {
        tenantContext = new TenantContext();
        tenantRegistry = mock(TenantRegistry.class);
        when(tenantRegistry.normalizeTenantId(anyString()))
                .thenAnswer(invocation -> invocation.<String>getArgument(0).trim().toUpperCase(Locale.ROOT));
        factory = new AuthenticationPropagationHeadersFactory(tenantContext, tenantRegistry);
    }

    @Test
    void propagatesAuthorizationAndTenantHeaders() {
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put("Authorization", List.of("Bearer token"));
        incomingHeaders.put("X-Tenant-Id", List.of("AR"));
        MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        factory.update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("Bearer token"), outgoingHeaders.get("Authorization"));
        assertEquals(List.of("AR"), outgoingHeaders.get("X-Tenant-Id"));
    }

    @Test
    void doesNotAddHeadersWhenTheyAreMissing() {
        MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        factory.update(new MultivaluedHashMap<>(), outgoingHeaders);

        assertEquals(0, outgoingHeaders.size());
    }

    @Test
    void propagatesResolvedTenantWhenIncomingHeaderIsMissing() {
        tenantContext.setTenantId("PNPG");
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.putSingle("Authorization", "test-authorization");

        MultivaluedMap<String, String> outgoingHeaders =
                factory.update(incomingHeaders, new MultivaluedHashMap<>());

        assertEquals("PNPG", outgoingHeaders.getFirst("X-Tenant-Id"));
        assertEquals("test-authorization", outgoingHeaders.getFirst("Authorization"));
    }

    @Test
    void normalizesMatchingTenantHeaders() {
        tenantContext.setTenantId("AR");
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.putSingle("X-Tenant-Id", " ar ");

        assertEquals("AR", factory.update(incomingHeaders, new MultivaluedHashMap<>()).getFirst("X-Tenant-Id"));
    }

    @Test
    void rejectsConflictingIncomingTenant() {
        tenantContext.setTenantId("AR");
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put("X-Tenant-Id", List.of("AR", "PNPG"));

        assertThrows(BadRequestException.class,
                () -> factory.update(incomingHeaders, new MultivaluedHashMap<>()));
    }

    @Test
    void rejectsConflictingOutgoingTenant() {
        tenantContext.setTenantId("AR");
        MultivaluedMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        outgoingHeaders.putSingle("X-Tenant-Id", "PNPG");

        assertThrows(BadRequestException.class,
                () -> factory.update(new MultivaluedHashMap<>(), outgoingHeaders));
    }

    @Test
    void rejectsUnsupportedTenant() {
        when(tenantRegistry.resolve("UNKNOWN")).thenThrow(new UnknownTenantException("UNKNOWN"));
        MultivaluedMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.putSingle("X-Tenant-Id", "UNKNOWN");

        assertThrows(UnknownTenantException.class,
                () -> factory.update(incomingHeaders, new MultivaluedHashMap<>()));
    }
}
