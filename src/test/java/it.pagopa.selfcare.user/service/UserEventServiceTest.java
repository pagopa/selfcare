package it.pagopa.selfcare.user.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.awaitility.Awaitility.await;

@QuarkusTest
public class UserEventServiceTest {
    @Inject
    @Any
    InMemoryConnector connector;
    @Inject
    UserEventService userEventService;

    @BeforeAll
    public static void switchMyChannels() {
        InMemoryConnector.switchOutgoingChannelsToInMemory("sc-users");
    }
    @Test
    void testSendUpdateUserNotificationToQueue() {
        InMemorySink<String> usersOut = connector.sink("sc-users");
        UniAssertSubscriber<Void> subscriber = userEventService.sendUpdateUserNotificationToQueue("userId", "institutionId")
                .subscribe().withSubscriber(UniAssertSubscriber.create());
        subscriber.assertCompleted();

        // Wait that the event is sent on kafka.
        await().<List<? extends Message<String>>>until(usersOut::received, t -> t.size() == 1);

        String queuedMessage = usersOut.received().get(0).getPayload();
        Assertions.assertTrue(queuedMessage.contains("userId"));
    }


}
