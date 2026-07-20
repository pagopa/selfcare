package it.pagopa.selfcare.webhook.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class WebhookNotificationRepositoryTest {

  @Inject WebhookNotificationRepository webhookNotificationRepository;

  @BeforeEach
  void setUp() {
    webhookNotificationRepository.deleteAll().await().indefinitely();
  }

  @Test
  void findAndLockPendingNotifications_shouldClaimOnlyEligibleNotifications() {
    // given
    ObjectId webhookId = new ObjectId();
    WebhookNotification eligiblePending =
        persistNotification(
            webhookId,
            WebhookNotification.NotificationStatus.PENDING,
            false,
            null,
            false,
            null,
            null);
    WebhookNotification eligibleRetry =
        persistNotification(
            webhookId,
            WebhookNotification.NotificationStatus.RETRY,
            false,
            null,
            false,
            null,
            null);
    WebhookNotification eligibleExpiredLock =
        persistNotification(
            webhookId,
            WebhookNotification.NotificationStatus.PENDING,
            true,
            LocalDateTime.now().minusMinutes(1),
            false,
            null,
            null);
    persistNotification(
        webhookId,
        WebhookNotification.NotificationStatus.SENDING,
        false,
        null,
        false,
        null,
        null);
    persistNotification(
        webhookId,
        WebhookNotification.NotificationStatus.PENDING,
        true,
        LocalDateTime.now().plusMinutes(5),
        false,
        null,
        null);

    // when
    List<WebhookNotification> claimed =
        webhookNotificationRepository.findAndLockPendingNotifications(10, 5).await().indefinitely();

    // then
    assertEquals(3, claimed.size());
    Set<ObjectId> claimedIds = claimed.stream().map(WebhookNotification::getId).collect(Collectors.toSet());
    assertTrue(claimedIds.contains(eligiblePending.getId()));
    assertTrue(claimedIds.contains(eligibleRetry.getId()));
    assertTrue(claimedIds.contains(eligibleExpiredLock.getId()));
    claimed.forEach(
        notification -> {
          assertTrue(notification.getProcessing());
          assertNotNull(notification.getProcessingUntil());
        });
  }

  @Test
  void releaseProcessingLock_shouldClearProcessingFields() {
    // given
    WebhookNotification notification =
        persistNotification(
            new ObjectId(),
            WebhookNotification.NotificationStatus.PENDING,
            true,
            LocalDateTime.now().plusMinutes(5),
            false,
            null,
            null);

    // when
    webhookNotificationRepository.releaseProcessingLock(notification).await().indefinitely();
    WebhookNotification updated =
        webhookNotificationRepository.findById(notification.getId()).await().indefinitely();

    // then
    assertNotNull(updated);
    assertFalse(updated.getProcessing());
    assertNull(updated.getProcessingUntil());
  }

  @Test
  void claimForProcessing_shouldClaimEligibleNotification() {
    // given
    WebhookNotification notification =
        persistNotification(
            new ObjectId(),
            WebhookNotification.NotificationStatus.PENDING,
            false,
            null,
            false,
            null,
            null);

    // when
    WebhookNotification claimed =
        webhookNotificationRepository
            .claimForProcessing(notification.getId().toHexString(), 5)
            .await()
            .indefinitely();

    // then
    assertNotNull(claimed);
    assertEquals(notification.getId(), claimed.getId());
    assertTrue(claimed.getProcessing());
    assertNotNull(claimed.getProcessingUntil());
  }

  @Test
  void claimForProcessing_shouldReturnNullWhenNotificationIsAlreadyLocked() {
    // given
    WebhookNotification notification =
        persistNotification(
            new ObjectId(),
            WebhookNotification.NotificationStatus.PENDING,
            true,
            LocalDateTime.now().plusMinutes(5),
            false,
            null,
            null);

    // when
    WebhookNotification claimed =
        webhookNotificationRepository
            .claimForProcessing(notification.getId().toHexString(), 5)
            .await()
            .indefinitely();

    // then
    assertNull(claimed);
  }

  @Test
  void markAsPublished_shouldUpdatePublicationMetadata() {
    // given
    WebhookNotification notification =
        persistNotification(
            new ObjectId(),
            WebhookNotification.NotificationStatus.PENDING,
            false,
            null,
            true,
            LocalDateTime.now().plusMinutes(5),
            null);

    // when
    webhookNotificationRepository.markAsPublished(notification.getId()).await().indefinitely();
    WebhookNotification updated =
        webhookNotificationRepository.findById(notification.getId()).await().indefinitely();

    // then
    assertNotNull(updated);
    assertNotNull(updated.getBusPublishedAt());
    assertFalse(updated.getPublishing());
    assertNull(updated.getPublishingUntil());
  }

  @Test
  void releasePublishingLock_shouldClearPublishingFields() {
    // given
    WebhookNotification notification =
        persistNotification(
            new ObjectId(),
            WebhookNotification.NotificationStatus.PENDING,
            false,
            null,
            true,
            LocalDateTime.now().plusMinutes(5),
            null);

    // when
    webhookNotificationRepository.releasePublishingLock(notification.getId()).await().indefinitely();
    WebhookNotification updated =
        webhookNotificationRepository.findById(notification.getId()).await().indefinitely();

    // then
    assertNotNull(updated);
    assertFalse(updated.getPublishing());
    assertNull(updated.getPublishingUntil());
  }

  @Test
  void claimUnpublishedNotifications_shouldClaimOnlyEligibleNotifications() {
    // given
    ObjectId webhookId = new ObjectId();
    WebhookNotification eligiblePending =
        persistNotification(
            webhookId,
            WebhookNotification.NotificationStatus.PENDING,
            false,
            null,
            false,
            null,
            null);
    WebhookNotification eligibleExpiredLock =
        persistNotification(
            webhookId,
            WebhookNotification.NotificationStatus.PENDING,
            false,
            null,
            true,
            LocalDateTime.now().minusMinutes(1),
            null);
    persistNotification(
        webhookId,
        WebhookNotification.NotificationStatus.PENDING,
        false,
        null,
        false,
        null,
        LocalDateTime.now());
    persistNotification(
        webhookId,
        WebhookNotification.NotificationStatus.RETRY,
        false,
        null,
        false,
        null,
        null);
    persistNotification(
        webhookId,
        WebhookNotification.NotificationStatus.PENDING,
        false,
        null,
        true,
        LocalDateTime.now().plusMinutes(5),
        null);

    // when
    List<WebhookNotification> claimed =
        webhookNotificationRepository.claimUnpublishedNotifications(10, 5).await().indefinitely();

    // then
    assertEquals(2, claimed.size());
    Set<ObjectId> claimedIds = claimed.stream().map(WebhookNotification::getId).collect(Collectors.toSet());
    assertTrue(claimedIds.contains(eligiblePending.getId()));
    assertTrue(claimedIds.contains(eligibleExpiredLock.getId()));
    claimed.forEach(
        notification -> {
          assertTrue(notification.getPublishing());
          assertNotNull(notification.getPublishingUntil());
        });
  }

  @Test
  void findByWebhookId_shouldReturnOnlyNotificationsForWebhook() {
    // given
    ObjectId webhookId = new ObjectId();
    persistNotification(
        webhookId,
        WebhookNotification.NotificationStatus.PENDING,
        false,
        null,
        false,
        null,
        null);
    persistNotification(
        webhookId,
        WebhookNotification.NotificationStatus.RETRY,
        false,
        null,
        false,
        null,
        null);
    persistNotification(
        new ObjectId(),
        WebhookNotification.NotificationStatus.PENDING,
        false,
        null,
        false,
        null,
        null);

    // when
    List<WebhookNotification> notifications =
        webhookNotificationRepository.findByWebhookId(webhookId.toHexString()).await().indefinitely();

    // then
    assertEquals(2, notifications.size());
    assertTrue(notifications.stream().allMatch(n -> webhookId.equals(n.getWebhookId())));
  }

  private WebhookNotification persistNotification(
      ObjectId webhookId,
      WebhookNotification.NotificationStatus status,
      boolean processing,
      LocalDateTime processingUntil,
      boolean publishing,
      LocalDateTime publishingUntil,
      LocalDateTime busPublishedAt) {
    WebhookNotification notification = new WebhookNotification();
    notification.setId(new ObjectId());
    notification.setWebhookId(webhookId);
    notification.setTenantId("SELC");
    notification.setPayload(Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8)));
    notification.setStatus(status);
    notification.setAttemptCount(0);
    notification.setCreatedAt(LocalDateTime.now());
    notification.setProcessing(processing);
    notification.setProcessingUntil(processingUntil);
    notification.setPublishing(publishing);
    notification.setPublishingUntil(publishingUntil);
    notification.setBusPublishedAt(busPublishedAt);
    return webhookNotificationRepository.persist(notification).await().indefinitely();
  }
}
