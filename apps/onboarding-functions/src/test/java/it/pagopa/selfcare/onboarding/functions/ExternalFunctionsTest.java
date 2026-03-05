package it.pagopa.selfcare.onboarding.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.service.CheckOrganizationService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
 class ExternalFunctionsTest {
    @Inject
    ExternalFunctions function;
    @InjectMock
    CheckOrganizationService checkOrganizationService;

    static final String ACK_PAYLOAD_OK = "{\"message\":\"message\"}";
    static final String ACK_PAYLOAD_BLANK = "{\"message\": \"\"}";

    @Test
    void checkOrganizationTest() {
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("fiscalCode", "someFiscalCode");
        queryParams.put("vatNumber", "someVatNumber");
        queryParams.put("Authorization", "someToken");
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        when(checkOrganizationService.checkOrganization(any(), any(), any())).thenReturn(true);
        HttpResponseMessage responseMessage = function.checkOrganization(req, context);
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());
    }

    @Test
    void checkOrganizationFiscalCodeNullTest() {
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("vatNumber", "vatNumber");
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        HttpResponseMessage responseMessage = function.checkOrganization(req, context);
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());
    }

    @Test
    void checkOrganizationVatNumberNullTest() {
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("fiscalCode", "fiscalCode");
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        HttpResponseMessage responseMessage = function.checkOrganization(req, context);
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());
    }

    @Test
    void checkOrganizationAlreadyRegisteredNullTest() {
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("fiscalCode", "fiscalCode");
        queryParams.put("vatNumber", "vatNumber");
        doReturn(queryParams).when(req).getQueryParameters();
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        when(checkOrganizationService.checkOrganization(any(), any(), any())).thenReturn(false);
        HttpResponseMessage responseMessage = function.checkOrganization(req, context);
        assertEquals(HttpStatus.NOT_FOUND.value(), responseMessage.getStatusCode());
    }

    @Test
    void messageAcknowledgmentRequestBodyEmpty() {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doReturn(Optional.empty()).when(req).getBody();
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
        HttpResponseMessage responseMessage = function.messageAcknowledgment(req, "productId", "messageId", "status", context);
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());
        assertEquals("Request body cannot be empty.", responseMessage.getBody());
    }

    @Test
    void messageAcknowledgmentRequestBodyHasBlankMessageInPayload() {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doReturn(Optional.of(ACK_PAYLOAD_BLANK)).when(req).getBody();
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
        HttpResponseMessage responseMessage = function.messageAcknowledgment(req, "productId", "messageId", "status", context);
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());
        assertEquals("Field message is required in request body and cannot be blank.", responseMessage.getBody());
    }

    @Test
    void messageAcknowledgmentOkStatusIsAck() {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doReturn(Optional.of(ACK_PAYLOAD_OK)).when(req).getBody();
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
        HttpResponseMessage responseMessage = function.messageAcknowledgment(req, "productId", "messageId", "ACK", context);
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());
    }

    @Test
    void messageAcknowledgmentOkStatusIsNack() {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doReturn(Optional.of(ACK_PAYLOAD_OK)).when(req).getBody();
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
        HttpResponseMessage responseMessage = function.messageAcknowledgment(req, "productId", "messageId", "NACK", context);
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());
    }

    @Test
    void webhookTest_success() {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        String validJson = "{\"key\":\"value\"}";
        doReturn(Optional.of(validJson)).when(req).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        HttpResponseMessage response = function.webhookTest(req, context);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
    }

    @Test
    void webhookTest_invalidJson() {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        String invalidJson = "{invalid}";
        doReturn(Optional.of(invalidJson)).when(req).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        HttpResponseMessage response = function.webhookTest(req, context);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode());
    }

    @Test
    void webhookTest_emptyBody() {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        doReturn(Optional.empty()).when(req).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        HttpResponseMessage response = function.webhookTest(req, context);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode());
    }
}
