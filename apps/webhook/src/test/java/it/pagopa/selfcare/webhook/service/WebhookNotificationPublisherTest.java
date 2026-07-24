package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.azure.storage.queue.QueueClient;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WebhookNotificationPublisherTest {

  private WebhookNotificationPublisher publisher;
  private QueueClient client;

  @BeforeEach
  void setUp() {
    publisher = new WebhookNotificationPublisher();
    client = mock(QueueClient.class);
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
    verify(client, never()).sendMessage(any(String.class));
  }

  @Test
  void publish_shouldSendNotificationIdAsMessageText() throws ReflectiveOperationException {
    // given
    String notificationId = "notification-id";
    publisher.enabled = true;
    setClient(client);

    // when
    UniAssertSubscriber<Void> subscriber =
        publisher.publish(notificationId).subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitItem();
    verify(client).sendMessage(notificationId);
  }

  @Test
  void publish_shouldFailWhenClientIsNotInitialized() {
    // given
    publisher.enabled = true;

    // when
    UniAssertSubscriber<Void> subscriber =
        publisher.publish("notification-id").subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitFailure().assertFailedWith(NullPointerException.class, "not initialized");
  }

  @Test
  void publish_shouldPropagateClientFailure() throws ReflectiveOperationException {
    // given
    publisher.enabled = true;
    setClient(client);
    RuntimeException error = new RuntimeException("Storage Queue is unavailable");
    doThrow(error).when(client).sendMessage(any(String.class));

    // when
    UniAssertSubscriber<Void> subscriber =
        publisher.publish("notification-id").subscribe().withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitFailure().assertFailedWith(RuntimeException.class, "Storage Queue is unavailable");
  }

  @Test
  void start_shouldDoNothingWhenDisabled() {
    // given
    publisher.enabled = false;

    // when
    assertDoesNotThrow(() -> publisher.start(null));

    // then
    verify(client, never()).sendMessage(any(String.class));
  }

  @Test
  void buildClientBuilder_shouldUseConnectionStringWhenProvided() {
    // given
    publisher.connectionString =
        "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=dGVzdA==;EndpointSuffix=core.windows.net";
    publisher.queue = "webhook-notifications";

    // when
    assertDoesNotThrow(() -> publisher.buildClientBuilder());
  }

  private void setClient(QueueClient client) throws ReflectiveOperationException {
    Field field = WebhookNotificationPublisher.class.getDeclaredField("client");
    field.setAccessible(true);
    field.set(publisher, client);
  }
}
