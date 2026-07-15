package it.pagopa.selfcare.delegation.event.auth;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.client.ClientRequestContext;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
public class EventhubTokenAuthorizationTest {

    @Test
    void testGetSASToken() {
        final EventhubTokenAuthorization auth = new EventhubTokenAuthorization(URI.create("testURI"), "testKeyName", "testKey", Optional.empty());
        final ClientRequestContext clientRequestContext = mock(ClientRequestContext.class);

        final jakarta.ws.rs.core.MultivaluedHashMap<String, Object> headers = new jakarta.ws.rs.core.MultivaluedHashMap<>();
        when(clientRequestContext.getHeaders()).thenReturn(headers);

        assertDoesNotThrow(() -> auth.filter(clientRequestContext));
        assertTrue(headers.containsKey("Authorization"));
        assertTrue(headers.get("Authorization").get(0).toString().startsWith("SharedAccessSignature "));
        assertTrue(headers.get("Authorization").get(0).toString().contains("sr=testURI"));
        assertTrue(headers.get("Authorization").get(0).toString().contains("skn=testKeyName"));
        assertTrue(headers.get("Authorization").get(0).toString().contains("sig="));
        assertTrue(headers.get("Authorization").get(0).toString().contains("se="));
    }

}
