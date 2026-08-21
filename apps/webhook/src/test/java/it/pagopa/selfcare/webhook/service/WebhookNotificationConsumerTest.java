package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
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
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookNotificationConsumerTest {

  @Inject WebhookNotificationConsumer webhookNotificationConsumer;

  @InjectMock WebhookNotificationRepository notificationRepository;

  @InjectMock WebhookNotificationService notificationService;

  @InjectMock WebhookRepository webhookRepository;

  @InjectMock it.pagopa.selfcare.webhook.metrics.WebhookMetrics metrics;

  @Inject Vertx vertx;

  private QueueMessageItem message;
  private QueueClient client;
  private QueueClient poisonClient;
  private Object serviceInstance;

  @BeforeEach
  void setUp() throws ReflectiveOperationException {
    message = mock(QueueMessageItem.class);
    when(message.getMessageId()).thenReturn("message-id");
    when(message.getPopReceipt()).thenReturn("pop-receipt");
    serviceInstance = io.quarkus.arc.ClientProxy.unwrap(webhookNotificationConsumer);

    client = mock(QueueClient.class);
    poisonClient = mock(QueueClient.class);
    Field field = WebhookNotificationConsumer.class.getDeclaredField("client");
    field.setAccessible(true);
    field.set(serviceInstance, client);
    Field poisonField = WebhookNotificationConsumer.class.getDeclaredField("poisonClient");
    poisonField.setAccessible(true);
    poisonField.set(serviceInstance, poisonClient);
    Field vertxField = WebhookNotificationConsumer.class.getDeclaredField("vertx");
    vertxField.setAccessible(true);
    vertxField.set(serviceInstance, vertx);
    consumer().maxMessagesPerPoll = 32;
    consumer().visibilityTimeoutSeconds = 300;
    consumer().maxInFlight = 32;
    consumer().maxDequeueCount = 5;
    consumer().maxBatchesPerPoll = 4;
    consumer().processingLockMarginSeconds = 60;
    consumer().queue = "webhook-notifications";
    consumer().poisonQueue = java.util.Optional.empty();
    consumer().inFlight = new Semaphore(32);
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
    consumer.queue = "webhook-notifications";
    consumer.poisonQueue = java.util.Optional.empty();
    consumer.autoCreate = true;
    doReturn(clientBuilder).when(consumer).buildClientBuilder(anyString());
    when(clientBuilder.buildClient()).thenReturn(client);

    // when
    consumer.start(null);

    // then
    verify(client, times(2)).createIfNotExists();
  }

  @Test
  void start_shouldNotCreateQueueWhenAutoCreateIsDisabled() {
    // given
    QueueClientBuilder clientBuilder = mock(QueueClientBuilder.class);
    WebhookNotificationConsumer consumer = spy(new WebhookNotificationConsumer());
    consumer.enabled = true;
    consumer.queue = "webhook-notifications";
    consumer.poisonQueue = java.util.Optional.empty();
    consumer.autoCreate = false;
    doReturn(clientBuilder).when(consumer).buildClientBuilder(anyString());
    when(clientBuilder.buildClient()).thenReturn(client);

    // when
    consumer.start(null);

    // then
    verify(client, never()).createIfNotExists();
  }

  @Test
  void start_shouldNotFailWhenQueueCreationIsUnauthorized() {
    // given
    QueueClientBuilder clientBuilder = mock(QueueClientBuilder.class);
    WebhookNotificationConsumer consumer = spy(new WebhookNotificationConsumer());
    consumer.enabled = true;
    consumer.queue = "webhook-notifications";
    consumer.poisonQueue = java.util.Optional.empty();
    consumer.autoCreate = true;
    doReturn(clientBuilder).when(consumer).buildClientBuilder(anyString());
    when(clientBuilder.buildClient()).thenReturn(client);
    doThrow(mock(QueueStorageException.class)).when(client).createIfNotExists();

    // when / then
    assertDoesNotThrow(() -> consumer.start(null));
  }

  @Test
  void buildClientBuilder_shouldUseConnectionStringWhenProvided() {
    // given
    consumer().connectionString =
        "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=dGVzdA==;EndpointSuffix=core.windows.net";
    consumer().queue = "webhook-notifications";

    // when
    assertDoesNotThrow(() -> consumer().buildClientBuilder("webhook-notifications"));
  }

  @Test
  void buildClientBuilder_shouldUseManagedIdentityWhenConnectionStringIsNotProvided() {
    // given
    consumer().connectionString = "none";
    consumer().endpoint = "https://test.queue.core.windows.net";
    consumer().queue = "webhook-notifications";

    // when
    assertDoesNotThrow(() -> consumer().buildClientBuilder("webhook-notifications"));
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
  void poll_shouldSkipWhenAtInFlightCapacity() {
    consumer().enabled = true;
    consumer().maxInFlight = 32;
    consumer().inFlight.drainPermits();

    consumer().poll();

    verify(client, never()).receiveMessages(any(), any(Duration.class), isNull(), isNull());
  }

  @Test
  void poll_shouldReceiveOnlyRemainingInFlightCapacity() {
    PagedIterable<QueueMessageItem> messages = mock(PagedIterable.class);
    when(messages.iterator()).thenReturn(Collections.emptyIterator());
    when(client.receiveMessages(eq(4), eq(Duration.ofSeconds(300)), isNull(), isNull()))
        .thenReturn(messages);
    consumer().enabled = true;
    consumer().maxInFlight = 32;
    consumer().inFlight.acquireUninterruptibly(28);

    consumer().poll();

    verify(client).receiveMessages(4, Duration.ofSeconds(300), null, null);
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
    consumer().autoCreate = true;

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
    verify(metrics).recordDiscarded("invalid_notification_id");
  }

  @Test
  void processMessage_shouldProcessMessageWithValidNotificationId()
      throws ReflectiveOperationException {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.DELIVERED);
    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notification.getId().toHexString()));
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
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
    verify(metrics, timeout(1000)).recordClaim("queue", 1);
  }

  @Test
  void processMessage_shouldNotDispatchWhenInFlightCapacityIsExhausted()
      throws ReflectiveOperationException {
    String notificationId = new ObjectId().toHexString();
    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notificationId));
    consumer().inFlight.drainPermits();

    invokeProcessMessage(message);

    verify(notificationRepository, never()).claimForProcessing(eq(notificationId), eq(6));
    verify(notificationService, never()).processNotification(any(WebhookNotification.class));
    verify(client, never()).deleteMessage(any(), any());
    // The message must become visible again immediately instead of staying hidden for the whole
    // visibility timeout while capacity is freed within seconds.
    verify(client).updateMessage("message-id", "pop-receipt", null, Duration.ZERO);
  }

  @Test
  void processMessage_shouldNotFailWhenResettingVisibilityOfSkippedMessageFails()
      throws ReflectiveOperationException {
    String notificationId = new ObjectId().toHexString();
    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notificationId));
    consumer().inFlight.drainPermits();
    doThrow(new RuntimeException("storage unavailable"))
        .when(client)
        .updateMessage(any(), any(), isNull(), any(Duration.class));

    assertDoesNotThrow(() -> invokeProcessMessage(message));

    verify(notificationRepository, never()).claimForProcessing(eq(notificationId), eq(6));
  }

  @Test
  void processMessage_shouldReleaseInFlightPermitOnlyOnce() throws ReflectiveOperationException {
    // given a message whose delete throws inside the subscription callback, the permit must not
    // be given back twice: an inflated semaphore would silently break the concurrency limit.
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.DELIVERED);
    when(message.getBody())
        .thenReturn(com.azure.core.util.BinaryData.fromString(notification.getId().toHexString()));
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));
    doThrow(new RuntimeException("delete failed")).when(client).deleteMessage(any(), any());

    // when
    invokeProcessMessage(message);

    // then
    verify(client, timeout(1000)).deleteMessage("message-id", "pop-receipt");
    await(() -> consumer().inFlight.availablePermits() == 32);
    assertEquals(32, consumer().inFlight.availablePermits());
  }

  @Test
  void processNotification_shouldDeleteMessageWhenUnclaimedNotificationIsMissing() {
    // given
    String notificationId = new ObjectId().toHexString();
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(6)))
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
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(6)))
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
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(6)))
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
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
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
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
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
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
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
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
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

    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
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
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(6)))
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

    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
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

  @Test
  void processMessage_shouldMoveMessageToPoisonQueueWhenDequeueCountIsExceeded()
      throws ReflectiveOperationException {
    // given a message that kept failing: Storage Queues have no native dead-lettering, so it
    // would otherwise be redelivered until its TTL expires, burning in-flight capacity.
    String notificationId = new ObjectId().toHexString();
    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notificationId));
    when(message.getDequeueCount()).thenReturn(6L);
    when(notificationRepository.markAsPermanentlyFailed(any(ObjectId.class), anyString()))
        .thenReturn(Uni.createFrom().voidItem());

    // when
    invokeProcessMessage(message);

    // then
    verify(poisonClient, timeout(1000)).sendMessage(notificationId);
    verify(notificationRepository, timeout(1000))
        .markAsPermanentlyFailed(any(ObjectId.class), anyString());
    verify(client, timeout(1000)).deleteMessage("message-id", "pop-receipt");
    verify(notificationRepository, never()).claimForProcessing(eq(notificationId), eq(6));
    verify(metrics, timeout(1000)).recordDiscarded("max_dequeue_count");
    await(() -> consumer().inFlight.availablePermits() == 32);
    assertEquals(32, consumer().inFlight.availablePermits());
  }

  @Test
  void processMessage_shouldKeepMessageWhenPoisonQueueSendFails()
      throws ReflectiveOperationException {
    // given the copy to the poison queue fails: the message must stay in the delivery queue
    // instead of being lost.
    String notificationId = new ObjectId().toHexString();
    when(message.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString(notificationId));
    when(message.getDequeueCount()).thenReturn(6L);
    doThrow(new RuntimeException("poison queue unavailable")).when(poisonClient).sendMessage(anyString());

    // when
    invokeProcessMessage(message);

    // then
    await(() -> consumer().inFlight.availablePermits() == 32);
    verify(client, never()).deleteMessage(any(), any());
    verify(notificationRepository, never()).markAsPermanentlyFailed(any(ObjectId.class), anyString());
    assertEquals(32, consumer().inFlight.availablePermits());
  }

  @Test
  void processMessage_shouldStillProcessMessageAtTheDequeueCountThreshold()
      throws ReflectiveOperationException {
    // given the dequeue count is exactly at the limit: the message deserves its last attempt.
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.DELIVERED);
    when(message.getBody())
        .thenReturn(com.azure.core.util.BinaryData.fromString(notification.getId().toHexString()));
    when(message.getDequeueCount()).thenReturn(5L);
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));

    // when
    invokeProcessMessage(message);

    // then
    verify(notificationService, timeout(1000)).processNotification(notification);
    verify(poisonClient, never()).sendMessage(anyString());
  }

  @Test
  void processMessage_shouldNotBlockTheEventLoopWithStorageQueueCalls()
      throws ReflectiveOperationException {
    // given the delete is executed at the tail of the reactive pipeline: it must run on a worker
    // thread, never on the Vert.x event loop the delivery is dispatched on.
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.DELIVERED);
    when(message.getBody())
        .thenReturn(com.azure.core.util.BinaryData.fromString(notification.getId().toHexString()));
    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(6)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.releaseProcessingLock(eq(notification)))
        .thenReturn(Uni.createFrom().item(notification));
    java.util.concurrent.atomic.AtomicReference<String> deleteThread =
        new java.util.concurrent.atomic.AtomicReference<>();
    doAnswer(
            invocation -> {
              deleteThread.set(Thread.currentThread().getName());
              return null;
            })
        .when(client)
        .deleteMessage(any(), any());

    // when
    invokeProcessMessage(message);

    // then
    verify(client, timeout(1000)).deleteMessage("message-id", "pop-receipt");
    assertNotNull(deleteThread.get());
    assertFalse(
        deleteThread.get().contains("vert.x-eventloop"),
        "blocking Storage Queue delete ran on the event loop: " + deleteThread.get());
  }

  @Test
  void poll_shouldKeepDrainingWhileTheQueueReturnsFullBatches() {
    // given a backlog: a single tick must not be capped at one batch, otherwise throughput would
    // be limited to max-messages-per-poll / poll-interval regardless of the free capacity.
    consumer().enabled = true;
    consumer().maxMessagesPerPoll = 2;
    consumer().maxBatchesPerPoll = 3;
    QueueMessageItem invalid = mock(QueueMessageItem.class);
    when(invalid.getMessageId()).thenReturn("invalid-id");
    when(invalid.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString("not-an-objectid"));
    when(client.receiveMessages(eq(2), eq(Duration.ofSeconds(300)), isNull(), isNull()))
        .thenAnswer(invocation -> pagedIterable(invalid, invalid))
        .thenAnswer(invocation -> pagedIterable(invalid, invalid))
        .thenAnswer(invocation -> pagedIterable(invalid));

    // when
    consumer().poll();

    // then the third batch is partial, so the poll stops without using the last allowed batch
    verify(client, times(3)).receiveMessages(2, Duration.ofSeconds(300), null, null);
  }

  @Test
  void poll_shouldStopDrainingAtTheBatchLimit() {
    // given a queue that always returns full batches: the scheduler worker thread must not be
    // held indefinitely.
    consumer().enabled = true;
    consumer().maxMessagesPerPoll = 2;
    consumer().maxBatchesPerPoll = 2;
    QueueMessageItem invalid = mock(QueueMessageItem.class);
    when(invalid.getMessageId()).thenReturn("invalid-id");
    when(invalid.getBody()).thenReturn(com.azure.core.util.BinaryData.fromString("not-an-objectid"));
    when(client.receiveMessages(eq(2), eq(Duration.ofSeconds(300)), isNull(), isNull()))
        .thenAnswer(invocation -> pagedIterable(invalid, invalid));

    // when
    consumer().poll();

    // then
    verify(client, times(2)).receiveMessages(2, Duration.ofSeconds(300), null, null);
  }

  @Test
  void processingLockMinutes_shouldAlwaysOutlastTheVisibilityTimeout() {
    // The lock must survive longer than the message invisibility, otherwise a redelivered message
    // could be claimed while the previous attempt is still delivering, sending the webhook twice.
    consumer().visibilityTimeoutSeconds = 300;
    consumer().processingLockMarginSeconds = 60;
    assertEquals(6, consumer().processingLockMinutes());

    consumer().visibilityTimeoutSeconds = 60;
    consumer().processingLockMarginSeconds = 60;
    assertEquals(2, consumer().processingLockMinutes());

    consumer().visibilityTimeoutSeconds = 10;
    consumer().processingLockMarginSeconds = 0;
    assertEquals(1, consumer().processingLockMinutes());
  }

  @SuppressWarnings("unchecked")
  private PagedIterable<QueueMessageItem> pagedIterable(QueueMessageItem... items) {
    PagedIterable<QueueMessageItem> messages = mock(PagedIterable.class);
    when(messages.iterator()).thenReturn(java.util.Arrays.asList(items).iterator());
    return messages;
  }

  private void invokeProcessNotification(QueueMessageItem message, String notificationId) {
    assertTrue(consumer().inFlight.tryAcquire());
    assertDoesNotThrow(
        () -> {
          Method method =
              WebhookNotificationConsumer.class.getDeclaredMethod(
                  "processNotification",
                  QueueMessageItem.class,
                  String.class,
                  AtomicBoolean.class);
          method.setAccessible(true);
          method.invoke(serviceInstance, message, notificationId, new AtomicBoolean(false));
        });
  }

  private WebhookNotificationConsumer consumer() {
    return (WebhookNotificationConsumer) serviceInstance;
  }

  private void await(java.util.function.BooleanSupplier condition) {
    long deadline = System.currentTimeMillis() + 1000;
    while (System.currentTimeMillis() < deadline && !condition.getAsBoolean()) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
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
