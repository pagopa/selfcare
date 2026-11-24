package it.pagopa.selfcare.user.client.auth;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.tenant.TenantContext;
import it.pagopa.selfcare.tenant.UnknownTenantException;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class AuthenticationPropagationHeadersFactoryTest {

    @Inject AuthenticationPropagationHeadersFactory factory;
    @Inject TenantContext context;
    @BeforeEach void setup() { context.clear(); }

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
    void propagatesTrustedContextWithoutIncomingHeader() {
        context.setTenantId("PNPG");
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(AuthenticationPropagationHeadersFactory.AUTHORIZATION_HEADER, List.of("Bearer token"));
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        factory.update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("Bearer token"), outgoingHeaders.get(AuthenticationPropagationHeadersFactory.AUTHORIZATION_HEADER));
        assertEquals(List.of("PNPG"), outgoingHeaders.get(AuthenticationPropagationHeadersFactory.TENANT_HEADER));
    }

    @Test void rejectsConflictingInboundTenant() {
        context.setTenantId("AR");
        var incoming = new MultivaluedHashMap<String, String>();
        incoming.putSingle("X-Tenant-Id", "PNPG");
        assertThrows(BadRequestException.class, () -> factory.update(incoming, new MultivaluedHashMap<>()));
    }

    @Test void rejectsConflictingOutgoingTenant() {
        context.setTenantId("AR");
        var outgoing = new MultivaluedHashMap<String, String>();
        outgoing.putSingle("X-Tenant-Id", "PNPG");
        assertThrows(BadRequestException.class, () -> factory.update(new MultivaluedHashMap<>(), outgoing));
    }

    @Test void rejectsUnknownTenant() {
        var incoming = new MultivaluedHashMap<String, String>();
        incoming.putSingle("X-Tenant-Id", "UNKNOWN");
        assertThrows(UnknownTenantException.class, () -> factory.update(incoming, new MultivaluedHashMap<>()));
    }

    @Test void normalizesCaseInsensitiveTenantHeadersWithoutDuplicates() {
        context.setTenantId("AR");
        var incoming = new MultivaluedHashMap<String, String>();
        incoming.putSingle("x-tenant-id", "ar");
        var outgoing = new MultivaluedHashMap<String, String>();
        outgoing.putSingle("x-tenant-id", "AR");
        factory.update(incoming, outgoing);
        assertEquals(List.of("AR"), outgoing.get("X-Tenant-Id"));
        assertNull(outgoing.get("x-tenant-id"));
    }

    @Test void rejectsConflictingCaseInsensitiveTenantHeaders() {
        context.setTenantId("AR");
        var incoming = new MultivaluedHashMap<String, String>();
        incoming.putSingle("x-tenant-id", "PNPG");
        assertThrows(BadRequestException.class, () -> factory.update(incoming, new MultivaluedHashMap<>()));
    }
}
