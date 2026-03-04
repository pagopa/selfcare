package it.pagopa.selfcare.onboarding.client;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.client.auth.EventhubSasTokenAuthorization;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
class EventhubSasTokenAuthorizationTest {

    @Inject
    EventhubSasTokenAuthorization eventhubSasTokenAuthorization;

    @Test
    void filter() throws URISyntaxException {
        ClientRequestContext clientRequest = mock(ClientRequestContext.class);
        final URI uri = new URI("http://test.it/SC-Contracts-SAP");
        when(clientRequest.getUri()).thenReturn(uri);
        when(clientRequest.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        eventhubSasTokenAuthorization.filter(clientRequest);
        assertFalse(clientRequest.getHeaders().isEmpty());
    }
}
