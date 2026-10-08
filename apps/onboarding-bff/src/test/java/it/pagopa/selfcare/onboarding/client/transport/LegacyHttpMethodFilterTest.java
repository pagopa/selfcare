package it.pagopa.selfcare.onboarding.client.transport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.ClientRequestContext;
import java.net.ProtocolException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LegacyHttpMethodFilterTest {

    private final LegacyHttpMethodFilter filter = new LegacyHttpMethodFilter();

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"})
    void acceptsEveryLegacyMethod(String method) {
        assertDoesNotThrow(() -> filter.filter(request(method)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PATCH", "CONNECT", "patch"})
    void rejectsUnsupportedMethodsBeforeSendingTheRequest(String method) {
        ProtocolException failure = assertThrows(ProtocolException.class, () -> filter.filter(request(method)));
        assertEquals("Invalid HTTP method: " + method, failure.getMessage());
    }

    private static ClientRequestContext request(String method) {
        ClientRequestContext context = mock(ClientRequestContext.class);
        when(context.getMethod()).thenReturn(method);
        return context;
    }
}
