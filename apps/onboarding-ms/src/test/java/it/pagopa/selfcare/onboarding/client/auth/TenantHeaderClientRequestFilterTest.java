package it.pagopa.selfcare.onboarding.client.auth;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantHeaderClientRequestFilterTest {

    @Test
    void forwardsResolvedTenantToFunctionsClient() {
        // given
        AuthenticationPropagationHeadersFactory headersFactory = mock(AuthenticationPropagationHeadersFactory.class);
        TenantHeaderClientRequestFilter filter = new TenantHeaderClientRequestFilter(headersFactory);
        MultivaluedMap<String, String> resolvedHeaders = new MultivaluedHashMap<>();
        resolvedHeaders.putSingle("X-Tenant-Id", "AR");
        when(headersFactory.update(any(), any())).thenReturn(resolvedHeaders);
        MultivaluedMap<String, Object> requestHeaders = new MultivaluedHashMap<>();

        // when
        filter.filter(requestContext(requestHeaders));

        // then
        assertEquals(List.of("AR"), requestHeaders.get("X-Tenant-Id"));
    }

    private static ClientRequestContext requestContext(MultivaluedMap<String, Object> headers) {
        ClientRequestContext requestContext = mock(ClientRequestContext.class);
        when(requestContext.getHeaders()).thenReturn(headers);
        when(requestContext.getStringHeaders()).thenReturn(new MultivaluedHashMap<>());
        return requestContext;
    }
}
