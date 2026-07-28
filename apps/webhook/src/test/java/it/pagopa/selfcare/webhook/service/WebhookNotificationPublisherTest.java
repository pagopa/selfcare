package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookNotificationPublisherTest {

  @Inject WebhookNotificationPublisher webhookNotificationPublisher;

  private WebhookNotificationPublisher publisher;
  private QueueClient client;

  @BeforeEach
  void setUp() throws ReflectiveOperationException {
    publisher = io.quarkus.arc.ClientProxy.unwrap(webhookNotificationPublisher);
    client = mock(QueueClient.class);
    publisher.enabled = false;
    setClient(null);
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

  @Test
  void start_shouldCreateQueueWhenEnabled() {
    // given
    QueueClientBuilder clientBuilder = mock(QueueClientBuilder.class);
    publisher = spy(publisher);
    publisher.enabled = true;
    doReturn(clientBuilder).when(publisher).buildClientBuilder();
    when(clientBuilder.buildClient()).thenReturn(client);

    // when
    publisher.start(null);

    // then
    verify(client).createIfNotExists();
  }

  @Test
  void buildClientBuilder_shouldUseManagedIdentityWhenConnectionStringIsNotProvided() {
    // given
    publisher.connectionString = "none";
    publisher.endpoint = "https://test.queue.core.windows.net";
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
