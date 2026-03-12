package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.durabletask.DurableTaskClient;
import com.microsoft.durabletask.Task;
import com.microsoft.durabletask.TaskOrchestrationContext;
import com.microsoft.durabletask.azurefunctions.DurableClientContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.dto.NotificationCountResult;
import it.pagopa.selfcare.onboarding.dto.ResendNotificationsFilters;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.NotificationException;
import it.pagopa.selfcare.onboarding.service.NotificationEventResenderService;
import it.pagopa.selfcare.onboarding.service.NotificationEventService;
import it.pagopa.selfcare.onboarding.service.OnboardingService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.logging.Logger;

import static it.pagopa.selfcare.onboarding.functions.utils.ActivityName.RESEND_NOTIFICATIONS_ACTIVITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


/**
 * Unit test for Notification Function class.
 */
@QuarkusTest
public class NotificationFunctionsTest {

    @Inject
    NotificationFunctions function;

    @InjectMock
    OnboardingService onboardingService;

    @InjectMock
    NotificationEventService notificationEventService;

    @InjectMock
    NotificationEventResenderService notificationEventResenderService;


    final String onboardinString = "{\"onboardingId\":\"onboardingId\"}";

    static ExecutionContext executionContext;

    static {
        executionContext = mock(ExecutionContext.class);
        when(executionContext.getLogger()).thenReturn(Logger.getGlobal());
    }

    @Test
    public void sendNotificationTrigger() {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Optional<String> queryBody = Optional.of(onboardinString);
        doReturn(queryBody).when(req).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // Invoke
        HttpResponseMessage responseMessage = function.sendNotification(req, context);

        // Verify
        Mockito.verify(notificationEventService, times(1))
                .send(any(), any(), any());
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());

    }

    @Test
    public void resendNotificationTrigger() {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        final String onboardingId = "onboardingId";
        queryParams.put("onboardingId", onboardingId);
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        when(onboardingService.getOnboarding(onboardingId)).thenReturn(Optional.of(new Onboarding()));

        // Invoke
        HttpResponseMessage responseMessage = function.resendNotification(req, context);

        // Verify
        Mockito.verify(notificationEventService, times(1))
                .send(any(), any(), any());
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());

    }

    @Test
    public void resendNotificationNullOnboardingId() {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // Invoke
        HttpResponseMessage responseMessage = function.resendNotification(req, context);
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());

    }

    @Test
    public void resendNotificationOnboardingNotFound() {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        final String onboardingId = "onboardingId";
        queryParams.put("onboardingId", onboardingId);
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        when(onboardingService.getOnboarding(onboardingId)).thenReturn(Optional.empty());

        // Invoke
        HttpResponseMessage responseMessage = function.resendNotification(req, context);
        assertEquals(HttpStatus.NOT_FOUND.value(), responseMessage.getStatusCode());

    }

    @Test
    public void sendNotificationTriggerError() {
        // Setup
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final String malformedOnboarding = "{\"onboardingId\":\"onboardingId\"";
        final Optional<String> queryBody = Optional.of(malformedOnboarding);
        doReturn(queryBody).when(req).getBody();
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
        // Invoke
        HttpResponseMessage responseMessage = function.sendNotification(req, context);
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());

    }

    @Test
    public void countOnboardingShouldReturnOkWhenEmptyList() {
        // Given
        final String from = "from";
        final String to = "to";
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("productId", null);
        queryParams.put("from", from);
        queryParams.put("to", to);
        doReturn(queryParams).when(req).getQueryParameters();

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        when(onboardingService.countNotifications(null, from, to, context)).thenReturn(new ArrayList<>());

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // When
        HttpResponseMessage responseMessage = function.countNotifications(req, context);

        // Then
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());
        verify(onboardingService, times(1)).countNotifications(null, from, to, context);
    }


    @Test
    public void countOnboardingShouldReturnOkWithCorrectBodyWhenServiceReturnsData() {
        // Given
        final String productId = "productId";
        final String from = "from";
        final String to = "to";
        @SuppressWarnings("unchecked") final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("productId", productId);
        queryParams.put("from", from);
        queryParams.put("to", to);
        doReturn(queryParams).when(req).getQueryParameters();
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        List<NotificationCountResult> expectedResults = List.of(new NotificationCountResult("product1", 1L));
        when(onboardingService.countNotifications(productId, from, to, context)).thenReturn(expectedResults);

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        // When
        HttpResponseMessage responseMessage = function.countNotifications(req, context);

        // Then
        assertEquals(HttpStatus.OK.value(), responseMessage.getStatusCode());
        assertEquals(expectedResults, responseMessage.getBody());
        verify(onboardingService, times(1)).countNotifications(productId, from, to, context);
    }

    @Test
    void resendNotification_shouldCallOrchestratorAndTerminate() throws JsonProcessingException {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        final String filtersAsJson = "{\"productId\":\"prod-pagoPa\", \"status\":\"[COMPLETED]\"}";
        queryParams.put("productId", "prod-pagoPa");
        queryParams.put("status", "COMPLETED");
        doReturn(queryParams).when(req).getQueryParameters();

        final Optional<String> queryBody = Optional.empty();
        doReturn(queryBody).when(req).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        final DurableClientContext durableContext = mock(DurableClientContext.class);
        final DurableTaskClient client = mock(DurableTaskClient.class);
        final String scheduleNewOrchestrationInstance = "scheduleNewOrchestrationInstance";
        doReturn(client).when(durableContext).getClient();
        doReturn(scheduleNewOrchestrationInstance).when(client).scheduleNewOrchestrationInstance("NotificationsSender", filtersAsJson);
        when(durableContext.createCheckStatusResponse(any(), any())).thenReturn(new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(HttpStatus.ACCEPTED).build());

        // Invoke
        HttpResponseMessage responseMessage = function.resendNotifications(req, durableContext, context);

        // Verify
        assertEquals(HttpStatus.ACCEPTED.value(), responseMessage.getStatusCode());
    }

    @Test
    void resendNotification_shouldThrowBadRequestWhenFieldStatusIsNotAllowed() throws JsonProcessingException {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("productId", "prod-pagoPa");
        queryParams.put("status", "TEST");
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        final DurableClientContext durableContext = mock(DurableClientContext.class);

        // Invoke
        HttpResponseMessage responseMessage = function.resendNotifications(req, durableContext, context);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());
    }

    @Test
    void resendNotification_shouldThrowBadRequestWhenFieldsDateHaveWrongFormat() throws JsonProcessingException {
        final HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put("productId", "prod-pagoPa");
        queryParams.put("from", "TEST");
        queryParams.put("to", "TEST");
        doReturn(queryParams).when(req).getQueryParameters();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));

        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        final DurableClientContext durableContext = mock(DurableClientContext.class);

        // Invoke
        HttpResponseMessage responseMessage = function.resendNotifications(req, durableContext, context);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseMessage.getStatusCode());
    }

    @Test
    void notificationsSenderOrchestrator_invokeActivity() throws JsonProcessingException {
        TaskOrchestrationContext orchestrationContext = mock(TaskOrchestrationContext.class);
        String filtersString = "{\"productId\":\"prod-pagoPa\", \"status\":[\"COMPLETED\"]}";
        when(orchestrationContext.getInput(String.class)).thenReturn(filtersString);
        Task task = mock(Task.class);
        when(orchestrationContext.getInstanceId()).thenReturn("instanceId");
        String enrichedFiltersString = "{\"productId\":\"prod-pagoPa\",\"status\":[\"COMPLETED\"],\"notificationEventTraceId\":\"instanceId\"}";

        when(orchestrationContext.callActivity(RESEND_NOTIFICATIONS_ACTIVITY, enrichedFiltersString, String.class)).thenReturn(task);
        when(task.await()).thenReturn(null);
        function.notificationsSenderOrchestrator(orchestrationContext, executionContext);

        Mockito.verify(orchestrationContext, times(1))
                .callActivity(RESEND_NOTIFICATIONS_ACTIVITY, enrichedFiltersString, String.class);
    }

    @Test
    void resendNotificationsActivity_shouldResendNotificationsAndReturnNullFiltersIfThereArentMoreOnboardings() throws JsonProcessingException {
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        String filtersString = "{\"productId\":\"prod-pagopa\", \"status\":[\"COMPLETED\"]}";

        when(notificationEventResenderService.resendNotifications(any(), any())).thenReturn(null);

        String nextFilter = function.resendNotificationsActivity(filtersString, context);

        ArgumentCaptor<ResendNotificationsFilters> filtersStringCaptor = ArgumentCaptor.forClass(ResendNotificationsFilters.class);
        Mockito.verify(notificationEventResenderService, times(1))
                .resendNotifications(filtersStringCaptor.capture(), any());
        assertEquals("prod-pagopa", filtersStringCaptor.getValue().getProductId());
        assertNull(nextFilter);
    }

    @Test
    void resendNotificationsActivity_failsParsingFilters() {
        ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();
        String filtersString = "{\"productId\":\"prod-pagopa\", \"status\":\"[COMPLETED]\"}";


        Assertions.assertThrows(NotificationException.class, () -> function.resendNotificationsActivity(filtersString, context));

        Mockito.verifyNoInteractions(notificationEventResenderService);
    }
}

