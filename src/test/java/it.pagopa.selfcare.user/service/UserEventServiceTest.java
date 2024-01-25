package it.pagopa.selfcare.user.service;

import io.quarkus.test.InjectMock;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class UserEventServiceTest {
    @InjectMock
    MutinyEmitter<String> userEmitter;
    @Inject
    UserEventService userEventService;

    @Test
    void testSendUpdateUserNotificationToQueue() {
        when(userEmitter.sendMessage(any())).thenReturn(Uni.createFrom().nullItem());
        UniAssertSubscriber<Void> subscriber = userEventService.sendUpdateUserNotificationToQueue("userId", "institutionId")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();
    }
}
