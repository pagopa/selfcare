package it.pagopa.selfcare.contractscrm.client.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EventhubTokenAuthorizationTest {

  @Test
  void filter_withoutManagedIdentity_addsSasToken() throws Exception {
    EventhubTokenAuthorization authorization =
        new EventhubTokenAuthorization(
            URI.create("https://selc-d-eventhub-ns.servicebus.windows.net/sc-contractscrmdelegations"),
            "selfcare-wo",
            "secret",
            Optional.empty());
    ClientRequestContext requestContext = mock(ClientRequestContext.class);
    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    when(requestContext.getHeaders()).thenReturn(headers);

    authorization.filter(requestContext);

    String token = (String) headers.getFirst("Authorization");
    assertTrue(
        token.startsWith(
            "SharedAccessSignature sr=https%3A%2F%2Fselc-d-eventhub-ns.servicebus.windows.net%2Fsc-contractscrmdelegations&sig="));
    assertTrue(token.matches(".*&se=\\d+&skn=selfcare-wo$"));
  }
}
