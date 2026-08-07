package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.QueueStorageException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import it.pagopa.selfcare.webhook.entity.RetryPolicy;
import it.pagopa.selfcare.webhook.entity.Webhook;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import it.pagopa.selfcare.webhook.repository.WebhookRepository;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookNotificationConsumerTest {

  @Inject WebhookNotificationConsumer webhookNotificationConsumer;

  @InjectMock WebhookNotificationRepository notificationRepository;

  @InjectMock WebhookNotificationService notificationService;

  @InjectMock WebhookRepository webhookRepository;

  @Inject Vertx vertx;

  private QueueMessageItem message;
  private QueueClient client;
  private Object serviceInstance;

  @BeforeEach
  void setUp() throws ReflectiveOperationException {
    message = mock(QueueMessageItem.class);
    when(message.getMessageId()).thenReturn("message-id");
    when(message.getPopReceipt()).thenReturn("pop-receipt");
    serviceInstance = io.quarkus.arc.ClientProxy.unwrap(webhookNotificationConsumer);

    client = mock(QueueClient.class);
    Field field = WebhookNotificationConsumer.class.getDeclaredField("client");
    field.setAccessible(true);
    field.set(serviceInstance, client);
    Field vertxField = WebhookNotificationConsumer.class.getDeclaredField("vertx");
    vertxField.setAccessible(true);
    vertxField.set(serviceInstance, vertx);
    consumer().maxMessagesPerPoll = 32;
    consumer().visibilityTimeoutSeconds = 300;
  }

  @Test
  void start_shouldDoNothingWhenDisabled() {
    // given
    WebhookNotificationConsumer consumer = new WebhookNotificationConsumer();
    consumer.enabled = false;

    // when
    assertDoesNotThrow(() -> consumer.start(null));

    // then
    assertDoesNotThrow(consumer::poll);
  }

  @Test
  void start_shouldCreateQueueWhenEnabled() {
    // given
    QueueClientBuilder clientBuilder = mock(QueueClientBuilder.class);
    WebhookNotificationConsumer consumer = spy(new WebhookNotificationConsumer());
    consumer.enabled = true;
    doReturn(clientBuilder).when(consumer).buildClientBuilder();
    when(clientBuilder.buildClient()).thenReturn(client);

    // when
    consumer.start(null);

    // then
    verify(client).createIfNotExists();
  }

  @Test
  void buildClientBuilder_shouldUseConnectionStringWhenProvided() {
    // given
    consumer().connectionString =
        "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=dGVzdA==;EndpointSuffix=core.windows.net";
    consumer().queue = "webhook-notifications";

    // when
    assertDoesNotThrow(() -> consumer().buildClientBuilder());
  }

  @Test
  void buildClientBuilder_shouldUseManagedIdentityWhenConnectionStringIsNotProvided() {
    // given
    consumer().connectionString = "none";
    consumer().endpoint = "https://test.queue.core.windows.net";
    consumer().queue = "webhook-notifications";

    // when
    assertDoesNotThrow(() -> consumer().buildClientBuilder());
  }

  @Test
  void poll_shouldDoNothingWhenDisabled() {
    // given
    consumer().enabled = false;

    // when
    consumer().poll();

    // then
    verify(client, never())
        .receiveMessages(any(), any(Duration.class), isNull(), isNull());
  }

  @Test
  void poll_shouldDoNothingWhenClientIsNotInitialized() throws ReflectiveOperationException {
    // given
    consumer().enabled = true;
    setClient(null);

    // when
    consumer().poll();

    // then
    verify(client, never())
        .receiveMessages(any(), any(Duration.class), isNull(), isNull());
  }

  @Test
  void poll_shouldReceiveMessages() {
    // given
    PagedIterable<QueueMessageItem> messages = mock(PagedIterable.class);
    when(messages.iterator()).thenReturn(Collections.emptyIterator());
    when(client.receiveMessages(eq(32), eq(Duration.ofSeconds(300)), isNull(), isNull()))
        .thenReturn(messages);
    consumer().enabled = true;

    // when
    consumer().poll();
    // then
    // then
    verify(client).receiveMessages(32, Duration.ofSeconds(300), null, null);
  }

  @Test
  void poll_shouldRecreateQueueWhenItDoesNotExist() {
    // given
    QueueStorageException exception = mock(QueueStorageException.class);
    when(exception.getStatusCode()).thenReturn(404);
    doThrow(exception)
        .when(client)
        .receiveMessages(eq(32), eq(Duration.ofSeconds(300)), isNull(), isNull());
    consumer().enabled = true;

    // when
    consumer().poll();

    // then
    verify(client).createIfNotExists();
  }

  @Test
  void poll_shouldNotRecreateQueueForOtherStorageFailures() {
    // given
    QueueStorageException exception = mock(QueueStorageException.class);
    when(exception.getStatusCode()).thenReturn(500);
    doThrow(exception)
        .when(client)
        .receiveMessages(eq(32), eq(Duration.ofSeconds(300)), isNull(), isNull());
    consumer().enabled = true;

    // when
    consumer().poll();

    // then
    verify(client, never()).createIfNotExists();
  }

  @Test
  void poll_shouldHandleUnexpectedFailures() {
    // given
    doThrow(new RuntimeException("Storage Queue is unavailable"))
        .when(client)
        .receiveMessages(eq(32), eq(Duration.ofSeconds(300)), isNull(), isNull());
    consumer().enabled = true;

    // when
    consumer().poll();

    // then
    verify(client, never()).createIfNotExists();
  }

  @Test
  void processMessage_shouldDiscardMessageWithInvalidNotificationId()
      throws ReflectiveOperationException {
    // given
    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString("invalid-notification-id"));

    // when
    invokeProcessMessage(message);

    // then
    verify(client).deleteMessage("message-id", "pop-receipt");
    verify(notificationService, never()).processNotification(any(WebhookNotification.class));
  }

  @Test
  void processMessage_shouldProcessMessageWithValidNotificationId()
      throws ReflectiveOperationException {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.DELIVERED);
    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notification.getId().toHexString()));
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(5)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));

    // when
    invokeProcessMessage(message);

    // then
    verify(notificationService, timeout(1000)).processNotification(notification);
    verify(client, timeout(1000)).deleteMessage("message-id", "pop-receipt");
  }

  @Test
  void processNotification_shouldDeleteMessageWhenUnclaimedNotificationIsMissing() {
    // given
    String notificationId = new ObjectId().toHexString();
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(5)))
        .thenReturn(Uni.createFrom().nullItem());
    when(notificationRepository.findById(any(ObjectId.class))).thenReturn(Uni.createFrom().nullItem());

    // when
    invokeProcessNotification(message, notificationId);

    // then
    verify(notificationService, timeout(1000).times(0)).processNotification(any(WebhookNotification.class));
    verify(client, timeout(1000)).deleteMessage("message-id", "pop-receipt");
  }

  @Test
  void processNotification_shouldDeleteMessageWhenUnclaimedNotificationIsTerminal() {
    // given
    String notificationId = new ObjectId().toHexString();
    WebhookNotification existing = new WebhookNotification();
    existing.setStatus(WebhookNotification.NotificationStatus.DELIVERED);
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(5)))
        .thenReturn(Uni.createFrom().nullItem());
    when(notificationRepository.findById(any(ObjectId.class)))
        .thenReturn(Uni.createFrom().item(existing));

    // when
    invokeProcessNotification(message, notificationId);

    // then
    verify(notificationService, timeout(1000).times(0)).processNotification(any(WebhookNotification.class));
    verify(client, timeout(1000)).deleteMessage("message-id", "pop-receipt");
  }

  @Test
  void processNotification_shouldNotDeleteMessageWhenUnclaimedNotificationIsStillLocked() {
    // given
    // claimForProcessing returned null because another attempt still holds the active lock
    // (e.g. still being processed, or abandoned mid-flight before the lock could be released).
    // The message must be kept in the queue so the notification is not lost forever.
    String notificationId = new ObjectId().toHexString();
    WebhookNotification existing = new WebhookNotification();
    existing.setStatus(WebhookNotification.NotificationStatus.SENDING);
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(5)))
        .thenReturn(Uni.createFrom().nullItem());
    when(notificationRepository.findById(any(ObjectId.class)))
        .thenReturn(Uni.createFrom().item(existing));

    // when
    invokeProcessNotification(message, notificationId);

    // then
    verify(notificationService, timeout(1000).times(0)).processNotification(any(WebhookNotification.class));
    verify(client, never()).deleteMessage(any(), any());
  }

  @Test
  void processNotification_shouldApplyRetryBackoffAndNotDeleteMessageWhenStatusIsRetry() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setWebhookId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.RETRY);
    notification.setAttemptCount(1);

    Webhook webhook = new Webhook();
    RetryPolicy retryPolicy = new RetryPolicy();
    retryPolicy.setInitialDelayMs(1000L);
    retryPolicy.setMaxDelayMs(10000L);
    retryPolicy.setBackoffMultiplier(2.0);
    webhook.setRetryPolicy(retryPolicy);

    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notification.getId().toHexString()));
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(5)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));
    when(webhookRepository.findById(eq(notification.getWebhookId())))
        .thenReturn(Uni.createFrom().item(webhook));

    // when
    invokeProcessNotification(message, notification.getId().toHexString());

    // then
    verify(notificationService, timeout(1000)).processNotification(eq(notification));
    verify(notificationRepository, timeout(1000)).releaseProcessingLock(eq(notification));
    // attempt 1 => delay = initialDelayMs * multiplier^0 = 1000ms
    verify(client, timeout(1000))
        .updateMessage(
            "message-id", "pop-receipt", notification.getId().toHexString(), Duration.ofMillis(1000));
    verify(client, never()).deleteMessage(any(), any());
  }

  @Test
  void processNotification_shouldCapRetryBackoffAtMaxDelay() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setWebhookId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.RETRY);
    notification.setAttemptCount(5);

    Webhook webhook = new Webhook();
    RetryPolicy retryPolicy = new RetryPolicy();
    retryPolicy.setInitialDelayMs(1000L);
    retryPolicy.setMaxDelayMs(10000L);
    retryPolicy.setBackoffMultiplier(2.0);
    webhook.setRetryPolicy(retryPolicy);

    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notification.getId().toHexString()));
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(5)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));
    when(webhookRepository.findById(eq(notification.getWebhookId())))
        .thenReturn(Uni.createFrom().item(webhook));

    // when
    invokeProcessNotification(message, notification.getId().toHexString());

    // then
    // attempt 5 => uncapped delay = 1000 * 2^4 = 16000ms, capped to maxDelayMs = 10000ms
    verify(client, timeout(1000))
        .updateMessage(
            "message-id", "pop-receipt", notification.getId().toHexString(), Duration.ofMillis(10000));
  }

  @Test
  void processNotification_shouldUseDefaultRetryPolicyWhenWebhookIsMissing() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setWebhookId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.RETRY);
    notification.setAttemptCount(1);

    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notification.getId().toHexString()));
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(5)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));
    when(webhookRepository.findById(eq(notification.getWebhookId())))
        .thenReturn(Uni.createFrom().nullItem());

    // when
    invokeProcessNotification(message, notification.getId().toHexString());

    // then
    // default policy: initialDelayMs=1000, multiplier=2.0 => attempt 1 delay = 1000ms
    verify(client, timeout(1000))
        .updateMessage(
            "message-id", "pop-receipt", notification.getId().toHexString(), Duration.ofMillis(1000));
    verify(client, never()).deleteMessage(any(), any());
  }

  @Test
  void processNotification_shouldFallBackToDefaultDelayWhenWebhookLookupFails() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setWebhookId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.RETRY);
    notification.setAttemptCount(1);

    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notification.getId().toHexString()));
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(5)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));
    when(webhookRepository.findById(eq(notification.getWebhookId())))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("db unavailable")));

    // when
    invokeProcessNotification(message, notification.getId().toHexString());

    // then
    verify(client, timeout(1000))
        .updateMessage(
            "message-id", "pop-receipt", notification.getId().toHexString(), Duration.ofMillis(1000));
    verify(client, never()).deleteMessage(any(), any());
  }

  @Test
  void processNotification_shouldCompleteWhenStatusIsDelivered() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.DELIVERED);

    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(5)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));

    // when
    invokeProcessNotification(message, notification.getId().toHexString());

    // then
    verify(notificationService, timeout(1000)).processNotification(eq(notification));
    verify(notificationRepository, timeout(1000)).releaseProcessingLock(eq(notification));
    verify(client, timeout(1000)).deleteMessage("message-id", "pop-receipt");
  }

  @Test
  void processNotification_shouldNotDeleteMessageWhenClaimFails() {
    // given
    String notificationId = new ObjectId().toHexString();
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(5)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("claim failed")));

    // when
    invokeProcessNotification(message, notificationId);

    // then
    verify(notificationService, never()).processNotification(any(WebhookNotification.class));
    verify(client, never()).deleteMessage(any(), any());
  }

  @Test
  void processNotification_shouldReleaseLockAndNotDeleteMessageWhenProcessingFails() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.SENDING);

    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(5)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("delivery failed")));
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));

    // when
    invokeProcessNotification(message, notification.getId().toHexString());

    // then
    verify(notificationService, timeout(1000)).processNotification(eq(notification));
    // The lock must be released even when processing fails, otherwise the notification would
    // remain locked until the lock expires with the message already gone from the queue.
    verify(notificationRepository, timeout(1000)).releaseProcessingLock(eq(notification));
    verify(client, never()).deleteMessage(any(), any());
  }

  private void invokeProcessNotification(QueueMessageItem message, String notificationId) {
    assertDoesNotThrow(
        () -> {
          Method method =
              WebhookNotificationConsumer.class.getDeclaredMethod(
                  "processNotification", QueueMessageItem.class, String.class);
          method.setAccessible(true);
          method.invoke(serviceInstance, message, notificationId);
        });
  }

  private WebhookNotificationConsumer consumer() {
    return (WebhookNotificationConsumer) serviceInstance;
  }

  private void setClient(QueueClient client) throws ReflectiveOperationException {
    Field field = WebhookNotificationConsumer.class.getDeclaredField("client");
    field.setAccessible(true);
    field.set(serviceInstance, client);
  }

  private void invokeProcessMessage(QueueMessageItem message) throws ReflectiveOperationException {
    Method method =
        WebhookNotificationConsumer.class.getDeclaredMethod("processMessage", QueueMessageItem.class);
    method.setAccessible(true);
    method.invoke(serviceInstance, message);
  }
}
