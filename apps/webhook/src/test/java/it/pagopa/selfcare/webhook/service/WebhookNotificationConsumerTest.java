package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.models.QueueMessageItem;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import jakarta.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookNotificationConsumerTest {

  @Inject WebhookNotificationConsumer webhookNotificationConsumer;

  @InjectMock WebhookNotificationRepository notificationRepository;

  @InjectMock WebhookNotificationService notificationService;

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
  }

  @Test
  void processNotification_shouldDoNothingWhenNotificationIsNotClaimed() {
    // given
    String notificationId = new ObjectId().toHexString();
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(5)))
        .thenReturn(Uni.createFrom().nullItem());

    // when
    invokeProcessNotification(message, notificationId);

    // then
    verify(notificationService, timeout(1000).times(0)).processNotification(any(WebhookNotification.class));
    verify(client, timeout(1000)).deleteMessage("message-id", "pop-receipt");
  }

  @Test
  void processNotification_shouldNotDeleteMessageWhenStatusIsRetry() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.RETRY);

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
  void processNotification_shouldNotDeleteMessageWhenProcessingFails() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.SENDING);

    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(5)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("delivery failed")));

    // when
    invokeProcessNotification(message, notification.getId().toHexString());

    // then
    verify(notificationService, timeout(1000)).processNotification(eq(notification));
    verify(notificationRepository, never()).releaseProcessingLock(eq(notification));
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
}
