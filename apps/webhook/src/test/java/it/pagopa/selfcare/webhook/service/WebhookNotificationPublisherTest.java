package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WebhookNotificationPublisherTest {

  private WebhookNotificationPublisher publisher;
  private ServiceBusSenderClient sender;

  @BeforeEach
  void setUp() {
    publisher = new WebhookNotificationPublisher();
    sender = mock(ServiceBusSenderClient.class);
  }

  @Test
  void publish_shouldCompleteWithoutSendingWhenDisabled() {
    // given
    publisher.enabled = false;

    // when
    UniAssertSubscriber<Void> subscriber =
        publisher.publish("notification-id").subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitItem();
    verify(sender, never()).sendMessage(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void publish_shouldSendNotificationIdAsMessageBodyAndId() throws ReflectiveOperationException {
    // given
    String notificationId = "notification-id";
    publisher.enabled = true;
    setSender(sender);

    // when
    UniAssertSubscriber<Void> subscriber =
        publisher.publish(notificationId).subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitItem();
    ArgumentCaptor<ServiceBusMessage> captor = ArgumentCaptor.forClass(ServiceBusMessage.class);
    verify(sender).sendMessage(captor.capture());
    ServiceBusMessage message = captor.getValue();
    org.junit.jupiter.api.Assertions.assertEquals(notificationId, message.getMessageId());
    org.junit.jupiter.api.Assertions.assertEquals(notificationId, message.getBody().toString());
  }

  @Test
  void publish_shouldFailWhenSenderIsNotInitialized() {
    // given
    publisher.enabled = true;

    // when
    UniAssertSubscriber<Void> subscriber =
        publisher.publish("notification-id").subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitFailure().assertFailedWith(NullPointerException.class, "not initialized");
  }

  @Test
  void publish_shouldPropagateSenderFailure() throws ReflectiveOperationException {
    // given
    publisher.enabled = true;
    setSender(sender);
    RuntimeException error = new RuntimeException("Service Bus is unavailable");
    org.mockito.Mockito.doThrow(error).when(sender).sendMessage(org.mockito.ArgumentMatchers.any());

    // when
    UniAssertSubscriber<Void> subscriber =
        publisher.publish("notification-id").subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitFailure().assertFailedWith(RuntimeException.class, "Service Bus is unavailable");
  }

  @Test
  void start_shouldDoNothingWhenDisabled() {
    // given
    publisher.enabled = false;

    // when
    assertDoesNotThrow(() -> publisher.start(null));

    // then
    verify(sender, never()).sendMessage(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void stop_shouldCloseInitializedSender() throws ReflectiveOperationException {
    // given
    setSender(sender);

    // when
    publisher.stop();

    // then
    verify(sender).close();
  }

  private void setSender(ServiceBusSenderClient sender) throws ReflectiveOperationException {
    Field field = WebhookNotificationPublisher.class.getDeclaredField("sender");
    field.setAccessible(true);
    field.set(publisher, sender);
  }
}
