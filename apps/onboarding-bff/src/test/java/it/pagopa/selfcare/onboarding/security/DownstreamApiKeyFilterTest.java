package it.pagopa.selfcare.onboarding.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class DownstreamApiKeyFilterTest {

    @Test
    void configuredKeyReplacesExistingValuesWithoutAddingAFunctionsKey() {
        var headers = new MultivaluedHashMap<String, Object>();
        headers.addAll("x-api-key", "caller-key", "other-key");
        headers.add("X-Tenant-Id", "PNPG");
        ClientRequestContext request = mock(ClientRequestContext.class);
        when(request.getHeaders()).thenReturn(headers);
        var filter = new DownstreamApiKeyFilter();
        filter.apiKey = "configured-key";

        filter.filter(request);

        assertEquals(List.of("configured-key"), headers.get("x-api-key"));
        assertEquals(List.of("PNPG"), headers.get("X-Tenant-Id"));
        assertFalse(headers.containsKey("x-functions-key"));
    }
}
