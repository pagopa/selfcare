package it.pagopa.selfcare.onboarding.client.auth;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.service.JwtSessionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
        when(jwtSessionService.createJwt(any(), any())).thenReturn(null);
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        assertTrue(outgoingHeaders.containsKey("Authorization"));
    }

    /**
     * The downstream tenant filter rejects a request whose X-Tenant-Id header is not corroborated by
     * the token's tenant claim, so the header must only be sent when the minted token carries it.
     */
    @Test
    void sendsTheTenantHeaderWhenTheMintedTokenCarriesTheTenant() {
        FunctionTenantContext.set("PNPG");
        try {
            MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
            outgoingHeaders.put("user-uuid", List.of(UUID.randomUUID().toString()));
            when(jwtSessionService.createJwt(any(), any())).thenReturn("a.jwt.token");

            authenticationPropagationHeadersFactory.update(new MultivaluedHashMap<>(), outgoingHeaders);

            assertEquals(List.of("PNPG"), outgoingHeaders.get(FunctionTenantContext.TENANT_HEADER));
        } finally {
            FunctionTenantContext.clear();
        }
    }

    @Test
    void mintsTheTokenForTheTenantOfTheCurrentActivity() {
        FunctionTenantContext.set("AR");
        try {
            MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
            String uuid = UUID.randomUUID().toString();
            outgoingHeaders.put("user-uuid", List.of(uuid));
            when(jwtSessionService.createJwt(any(), any())).thenReturn("a.jwt.token");

            authenticationPropagationHeadersFactory.update(new MultivaluedHashMap<>(), outgoingHeaders);

            verify(jwtSessionService).createJwt(uuid, "AR");
        } finally {
            FunctionTenantContext.clear();
        }
    }

    /**
     * The env-provided machine token cannot be given a tenant claim, so sending the header alone
     * would produce exactly the header/claim mismatch the downstream filter rejects.
     */
    @Test
    void omitsTheTenantHeaderWhenFallingBackToTheMachineToken() {
        FunctionTenantContext.set("AR");
        try {
            MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
            outgoingHeaders.put("user-uuid", List.of(UUID.randomUUID().toString()));
            when(jwtSessionService.createJwt(any(), any())).thenReturn(null);

            authenticationPropagationHeadersFactory.update(new MultivaluedHashMap<>(), outgoingHeaders);

            assertFalse(outgoingHeaders.containsKey(FunctionTenantContext.TENANT_HEADER));
        } finally {
            FunctionTenantContext.clear();
        }
    }

    @Test
    void omitsTheTenantHeaderWhenNoTenantIsKnown() {
        FunctionTenantContext.clear();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        outgoingHeaders.put("user-uuid", List.of(UUID.randomUUID().toString()));
        when(jwtSessionService.createJwt(any(), any())).thenReturn("a.jwt.token");

        authenticationPropagationHeadersFactory.update(new MultivaluedHashMap<>(), outgoingHeaders);

        assertFalse(outgoingHeaders.containsKey(FunctionTenantContext.TENANT_HEADER));
    }

    @Test
    void emptyHeader() {
        MultivaluedHashMap<String, String> incomingHeaders = new MultivaluedHashMap<>();
        MultivaluedHashMap<String, String> outgoingHeaders = new MultivaluedHashMap<>();
        authenticationPropagationHeadersFactory.update(incomingHeaders, outgoingHeaders);
        assertTrue(outgoingHeaders.containsKey("Authorization"));
    }

}
