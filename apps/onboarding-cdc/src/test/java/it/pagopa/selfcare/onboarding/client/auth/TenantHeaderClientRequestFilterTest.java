package it.pagopa.selfcare.onboarding.client.auth;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class TenantHeaderClientRequestFilterTest {

    @Inject
    TenantHeaderClientRequestFilter filter;

    @Test
    void sendsStackDefaultTenantAndTechnicalAuthorization() {
        // given
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        // when
        filter.filter(requestContext(headers));

        // then
        assertEquals(List.of("PNPG"), headers.get(TenantContext.TENANT_HEADER));
        assertTechnicalAuthorization(headers);
        assertEquals(List.of("test-function-key"), headers.get("x-functions-key"));
    }

    @Test
    void sendsTenantOfCurrentContext() {
        // given
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

        // when
        try (TenantContext.Scope ignored = TenantContext.open("PNPG")) {
            filter.filter(requestContext(headers));
        }

        // then
        assertEquals(List.of("PNPG"), headers.get(TenantContext.TENANT_HEADER));
        assertTechnicalAuthorization(headers);
        assertEquals(List.of("test-function-key"), headers.get("x-functions-key"));
    }

    private static ClientRequestContext requestContext(MultivaluedMap<String, Object> headers) {
        ClientRequestContext requestContext = mock(ClientRequestContext.class);
        when(requestContext.getHeaders()).thenReturn(headers);
        when(requestContext.getStringHeaders()).thenReturn(new MultivaluedHashMap<>());
        return requestContext;
    }

    private static void assertTechnicalAuthorization(MultivaluedMap<String, Object> headers) {
        assertTrue(headers.getFirst("Authorization").toString().startsWith("Bearer eyJ"));
    }
}
