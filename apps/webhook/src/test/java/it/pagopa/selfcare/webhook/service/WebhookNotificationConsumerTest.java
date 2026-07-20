package it.pagopa.selfcare.webhook.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import jakarta.inject.Inject;
import java.lang.reflect.Method;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookNotificationConsumerTest {

  @Inject WebhookNotificationConsumer webhookNotificationConsumer;

  @InjectMock WebhookNotificationRepository notificationRepository;

  @InjectMock WebhookNotificationService notificationService;

  private ServiceBusReceivedMessageContext messageContext;
  private Object serviceInstance;

  @BeforeEach
  void setUp() {
    messageContext = org.mockito.Mockito.mock(ServiceBusReceivedMessageContext.class);
    serviceInstance = io.quarkus.arc.ClientProxy.unwrap(webhookNotificationConsumer);
  }

  @Test
  void processNotification_shouldCompleteWhenNotificationIsNotClaimed() {
    // given
    String notificationId = new ObjectId().toHexString();
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(5)))
        .thenReturn(Uni.createFrom().nullItem());

    // when
    invokeProcessNotification(messageContext, notificationId);

    // then
    verify(messageContext, timeout(1000)).complete();
    verify(messageContext, never()).abandon();
  }

  @Test
  void processNotification_shouldAbandonWhenStatusIsRetry() {
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
    invokeProcessNotification(messageContext, notification.getId().toHexString());

    // then
    verify(notificationService, timeout(1000)).processNotification(eq(notification));
    verify(notificationRepository, timeout(1000)).releaseProcessingLock(eq(notification));
    verify(messageContext, timeout(1000)).abandon();
    verify(messageContext, never()).complete();
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
    invokeProcessNotification(messageContext, notification.getId().toHexString());

    // then
    verify(notificationService, timeout(1000)).processNotification(eq(notification));
    verify(notificationRepository, timeout(1000)).releaseProcessingLock(eq(notification));
    verify(messageContext, timeout(1000)).complete();
    verify(messageContext, never()).abandon();
  }

  @Test
  void processNotification_shouldAbandonWhenClaimFails() {
    // given
    String notificationId = new ObjectId().toHexString();
    when(notificationRepository.claimForProcessing(eq(notificationId), eq(5)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("claim failed")));

    // when
    invokeProcessNotification(messageContext, notificationId);

    // then
    verify(messageContext, timeout(1000)).abandon();
    verify(messageContext, never()).complete();
    verify(notificationService, never()).processNotification(any(WebhookNotification.class));
  }

  @Test
  void processNotification_shouldAbandonWhenProcessingFails() {
    // given
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setStatus(WebhookNotification.NotificationStatus.SENDING);

    when(notificationRepository.claimForProcessing(eq(notification.getId().toHexString()), eq(5)))
        .thenReturn(Uni.createFrom().item(notification));
    when(notificationService.processNotification(eq(notification)))
        .thenReturn(Uni.createFrom().failure(new RuntimeException("delivery failed")));

    // when
    invokeProcessNotification(messageContext, notification.getId().toHexString());

    // then
    verify(notificationService, timeout(1000)).processNotification(eq(notification));
    verify(messageContext, timeout(1000)).abandon();
    verify(messageContext, never()).complete();
    verify(notificationRepository, never()).releaseProcessingLock(eq(notification));
  }

  private void invokeProcessNotification(
      ServiceBusReceivedMessageContext context, String notificationId) {
    assertDoesNotThrow(
        () -> {
          Method method =
              WebhookNotificationConsumer.class.getDeclaredMethod(
                  "processNotification", ServiceBusReceivedMessageContext.class, String.class);
          method.setAccessible(true);
          method.invoke(serviceInstance, context, notificationId);
        });
  }
}
