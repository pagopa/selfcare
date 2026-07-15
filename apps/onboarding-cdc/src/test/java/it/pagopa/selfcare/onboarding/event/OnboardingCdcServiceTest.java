package it.pagopa.selfcare.onboarding.event;

import com.azure.data.tables.TableClient;
import com.microsoft.applicationinsights.TelemetryClient;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.event.entity.Onboarding;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.bson.BsonDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class OnboardingCdcServiceTest {

    @InjectMock
    private NotificationService notificationService;

    @InjectMock
    private RegistryIndexService registryIndexService;

    @InjectMock
    private TelemetryClient telemetryClient;

    @InjectMock
    private TableClient tableClient;

    @Inject
    private OnboardingCdcService onboardingCdcService;

    @Test
    @DisplayName("Should consume onboarding event successfully")
    void consumerOnboardingEventShouldHandleSuccess() {
        // given
        ChangeStreamDocument<Onboarding> document = mockChangeStreamDocument();
        when(notificationService.invokeNotificationApi(any(Onboarding.class)))
                .thenReturn(Uni.createFrom().item(new OrchestrationResponse()));

        // when
        onboardingCdcService.consumerOnboardingEvent(document);
        waitForAsyncCompletion();

        // then
        verify(notificationService, times(1)).invokeNotificationApi(any(Onboarding.class));
        verify(tableClient, times(1)).upsertEntity(any());
        verify(telemetryClient, times(1)).trackEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Should consume onboarding event failure")
    void consumerOnboardingEventShouldHandleFailure() {
        // given
        ChangeStreamDocument<Onboarding> document = mockChangeStreamDocument();
        when(notificationService.invokeNotificationApi(any(Onboarding.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("error")));

        // when
        onboardingCdcService.consumerOnboardingEvent(document);
        waitForAsyncCompletion();

        // then
        verify(notificationService, times(1)).invokeNotificationApi(any(Onboarding.class));
        verify(tableClient, never()).upsertEntity(any());
        verify(telemetryClient, times(1)).trackEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Should consume registry index event successfully")
    void consumerRegistryIndexEventShouldHandleSuccess() {
        // given
        ChangeStreamDocument<Onboarding> document = mockChangeStreamDocument();
        when(registryIndexService.updateIndex(any(Onboarding.class)))
                .thenReturn(Uni.createFrom().item(Response.noContent().build()));

        // when
        onboardingCdcService.consumerRegistryIndexEvent(document);
        waitForAsyncCompletion();

        // then
        verify(registryIndexService, times(1)).updateIndex(any(Onboarding.class));
        verify(telemetryClient, times(1)).trackEvent(any(), any(), any());
    }

    @Test
    @DisplayName("Should consume registry index event failure")
    void consumerRegistryIndexEventShouldHandleFailure() {
        // given
        ChangeStreamDocument<Onboarding> document = mockChangeStreamDocument();
        when(registryIndexService.updateIndex(any(Onboarding.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("error")));

        // when
        onboardingCdcService.consumerRegistryIndexEvent(document);
        waitForAsyncCompletion();

        // then
        verify(registryIndexService, times(1)).updateIndex(any(Onboarding.class));
        verify(telemetryClient, times(1)).trackEvent(any(), any(), any());
    }

    private ChangeStreamDocument<Onboarding> mockChangeStreamDocument() {
        Onboarding onboarding = new Onboarding();
        onboarding.id = "test-id";
        onboarding.setStatus(OnboardingStatus.COMPLETED);
        onboarding.setUpdatedAt(LocalDateTime.now());

        ChangeStreamDocument<Onboarding> document = mock(ChangeStreamDocument.class);
        when(document.getFullDocument()).thenReturn(onboarding);
        when(document.getDocumentKey()).thenReturn(BsonDocument.parse("{\"_id\": \"test-id\"}"));
        when(document.getResumeToken()).thenReturn(BsonDocument.parse("{\"token\": \"test-token\"}"));
        return document;
    }

    private void waitForAsyncCompletion() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e);
        }
    }
}
