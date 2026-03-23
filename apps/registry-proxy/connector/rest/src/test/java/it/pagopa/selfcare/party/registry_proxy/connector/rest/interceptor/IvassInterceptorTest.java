package it.pagopa.selfcare.party.registry_proxy.connector.rest.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

class IvassInterceptorTest {
    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    @InjectMocks
    private IvassInterceptor ivassInterceptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void intercept_shouldLogRequestAndResponseDetails() throws IOException {
        byte[] body = new byte[]{};
        when(execution.execute(request, body)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        ClientHttpResponse result = ivassInterceptor.intercept(request, body, execution);

        assertEquals(response, result);
        verify(request, times(1)).getURI();
        verify(request, times(1)).getHeaders();
        verify(request, times(1)).getMethod();
        verify(response, times(1)).getStatusCode();
        verify(response, times(1)).getHeaders();
    }

    @Test
    void intercept_shouldHandleIOException() throws IOException {
        byte[] body = new byte[]{};
        when(execution.execute(request, body)).thenThrow(new IOException("Test exception"));

        assertThrows(IOException.class, () -> ivassInterceptor.intercept(request, body, execution));
    }
}