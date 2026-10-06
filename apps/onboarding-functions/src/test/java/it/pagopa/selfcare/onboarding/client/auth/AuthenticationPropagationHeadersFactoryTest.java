package it.pagopa.selfcare.onboarding.client.auth;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import it.pagopa.selfcare.onboarding.service.JwtSessionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class AuthenticationPropagationHeadersFactoryTest {

    @Inject
    AuthenticationPropagationHeadersFactory authenticationPropagationHeadersFactory;

    @InjectMock
    JwtSessionService jwtSessionService;

    @Test
    void update() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(TenantContext.TENANT_HEADER, List.of("AR"));
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        outgoingHeaders.put("user-uuid", List.of(UUID.randomUUID().toString()));
        when(jwtSessionService.createJwt(any())).thenReturn("jwt");
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        assertTrue(outgoingHeaders.containsKey("Authorization"));
        assertEquals(List.of("AR"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
    }

    @Test
    void updateWithNullJwt() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(TenantContext.TENANT_HEADER, List.of("AR"));
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        outgoingHeaders.put("user-uuid", List.of(UUID.randomUUID().toString()));
        when(jwtSessionService.createJwt(any())).thenReturn(null);
        when(jwtSessionService.createMachineJwt()).thenReturn("machine-jwt");
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        assertEquals(List.of("Bearer machine-jwt"), outgoingHeaders.get("Authorization"));
    }

    @Test
    void rejectsMissingTenant() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        assertThrows(
                IllegalArgumentException.class,
                () -> authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders));
    }

    @Test
    void propagatesIncomingTenantHeader() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(TenantContext.TENANT_HEADER, List.of("AR"));
        when(jwtSessionService.createMachineJwt()).thenReturn("machine-jwt");

        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("AR"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
        assertEquals(List.of("Bearer machine-jwt"), outgoingHeaders.get("Authorization"));
    }

    @Test
    void propagatesTenantFromFunctionContext() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        when(jwtSessionService.createMachineJwt()).thenReturn("machine-jwt");

        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        }

        assertEquals(List.of("PNPG"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
    }

}
