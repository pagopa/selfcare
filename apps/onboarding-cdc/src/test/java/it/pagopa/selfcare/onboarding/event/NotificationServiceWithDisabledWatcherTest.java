package it.pagopa.selfcare.onboarding.event;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.mongodb.MongoTestResource;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.event.entity.Onboarding;
import it.pagopa.selfcare.onboarding.event.profile.NotificationTestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.onboarding_functions_json.api.NotificationsApi;
import org.openapi.quarkus.onboarding_functions_json.model.OrchestrationResponse;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
@TestProfile(NotificationTestProfile.class)
class NotificationServiceWithDisabledWatcherTest {

    @InjectMock
    @RestClient
    private NotificationsApi notificationsApi;

    @Inject
    private NotificationService notificationService;

    @Test
    @DisplayName("Should not invoke Notification API when watcher is disabled")
    void shouldNotInvokeNotificationApiWhenWatcherIsDisabled() {
        // given
        Onboarding onboarding = new Onboarding();
        onboarding.setStatus(OnboardingStatus.DELETED);
        onboarding.setUpdatedAt(LocalDateTime.now());
        onboarding.setActivatedAt(LocalDateTime.now());

        // when
        UniAssertSubscriber<OrchestrationResponse> subscriber = notificationService
                .invokeNotificationApi(onboarding)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertCompleted().awaitItem();
        verify(notificationsApi, times(0)).apiNotificationPost(any(), any());
    }
}
