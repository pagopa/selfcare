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
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        outgoingHeaders.put("user-uuid", List.of(UUID.randomUUID().toString()));
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        assertTrue(outgoingHeaders.containsKey("Authorization"));
    }

    @Test
    void updateWithNullJwt() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        outgoingHeaders.put("user-uuid", List.of(UUID.randomUUID().toString()));
        when(jwtSessionService.createJwt(any())).thenReturn(null);
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        assertTrue(outgoingHeaders.containsKey("Authorization"));
    }

    @Test
    void emptyHeader() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        assertTrue(outgoingHeaders.containsKey("Authorization"));
    }

    @Test
    void propagatesIncomingTenantHeader() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        incomingHeaders.put(TenantContext.TENANT_HEADER, List.of("AR"));

        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);

        assertEquals(List.of("AR"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
    }

    @Test
    void propagatesTenantFromFunctionContext() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();

        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        }

        assertEquals(List.of("PNPG"), outgoingHeaders.get(TenantContext.TENANT_HEADER));
    }

}
