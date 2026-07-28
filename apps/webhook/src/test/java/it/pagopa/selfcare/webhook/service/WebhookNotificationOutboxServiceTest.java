package it.pagopa.selfcare.webhook.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.repository.WebhookNotificationRepository;
import jakarta.inject.Inject;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WebhookNotificationOutboxServiceTest {

  @Inject WebhookNotificationOutboxService outboxService;

  @InjectMock WebhookNotificationRepository notificationRepository;

  @InjectMock WebhookNotificationPublisher publisher;

  private WebhookNotificationOutboxService serviceInstance;

  @BeforeEach
  void setUp() {
    serviceInstance = io.quarkus.arc.ClientProxy.unwrap(outboxService);
    serviceInstance.enabled = true;
  }

  @Test
  void publishUnpublishedNotifications_shouldNotQueryRepositoryWhenDisabled() {
    // given
    serviceInstance.enabled = false;

    // when
    UniAssertSubscriber<Void> subscriber =
        serviceInstance
            .publishUnpublishedNotifications()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitItem();
    verify(notificationRepository, never()).claimUnpublishedNotifications(any(Integer.class), any(Integer.class));
  }

  @Test
  void publishUnpublishedNotifications_shouldCompleteWhenNoNotificationsAreClaimed() {
    // given
    when(notificationRepository.claimUnpublishedNotifications(100, 5))
        .thenReturn(Uni.createFrom().item(List.of()));

    // when
    UniAssertSubscriber<Void> subscriber =
        serviceInstance
            .publishUnpublishedNotifications()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitItem();
    verify(notificationRepository).claimUnpublishedNotifications(100, 5);
    verify(publisher, never()).publish(any());
  }

  @Test
  void publishUnpublishedNotifications_shouldPublishAndMarkNotificationAsPublished() {
    // given
    WebhookNotification notification = notification();
    when(notificationRepository.claimUnpublishedNotifications(100, 5))
        .thenReturn(Uni.createFrom().item(List.of(notification)));
    when(publisher.publish(notification.getId().toHexString())).thenReturn(Uni.createFrom().voidItem());
    when(notificationRepository.markAsPublished(notification.getId()))
        .thenReturn(Uni.createFrom().voidItem());

    // when
    UniAssertSubscriber<Void> subscriber =
        serviceInstance
            .publishUnpublishedNotifications()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitItem();
    verify(publisher).publish(notification.getId().toHexString());
    verify(notificationRepository).markAsPublished(notification.getId());
    verify(notificationRepository, never()).releasePublishingLock(notification.getId());
  }

  @Test
  void publishUnpublishedNotifications_shouldReleasePublishingLockWhenPublishFails() {
    // given
    WebhookNotification notification = notification();
    RuntimeException error = new RuntimeException("Storage Queue is unavailable");
    when(notificationRepository.claimUnpublishedNotifications(100, 5))
        .thenReturn(Uni.createFrom().item(List.of(notification)));
    when(publisher.publish(notification.getId().toHexString()))
        .thenReturn(Uni.createFrom().failure(error));
    when(notificationRepository.releasePublishingLock(notification.getId()))
        .thenReturn(Uni.createFrom().voidItem());

    // when
    UniAssertSubscriber<Void> subscriber =
        serviceInstance
            .publishUnpublishedNotifications()
            .subscribe()
            .withSubscriber(UniAssertSubscriber.create());

    // then
    subscriber.awaitFailure().assertFailedWith(RuntimeException.class, "Storage Queue is unavailable");
    verify(notificationRepository).releasePublishingLock(notification.getId());
    verify(notificationRepository, never()).markAsPublished(eq(notification.getId()));
  }

  private WebhookNotification notification() {
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    return notification;
  }
}
