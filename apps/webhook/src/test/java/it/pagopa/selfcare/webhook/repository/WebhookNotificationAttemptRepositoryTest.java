package it.pagopa.selfcare.webhook.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import it.pagopa.selfcare.webhook.entity.WebhookNotification;
import it.pagopa.selfcare.webhook.entity.WebhookNotificationAttempt;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = MongoTestResource.class, restrictToAnnotatedClass = true)
class WebhookNotificationAttemptRepositoryTest {

  @Inject WebhookNotificationAttemptRepository webhookNotificationAttemptRepository;

  @BeforeEach
  void setUp() {
    webhookNotificationAttemptRepository.deleteAll().await().indefinitely();
  }

  @Test
  void findByNotificationId_shouldReturnOnlyAttemptsForNotificationInAttemptOrder() {
    ObjectId notificationId = new ObjectId();
    persistAttempt(notificationId, 2);
    persistAttempt(new ObjectId(), 1);
    persistAttempt(notificationId, 1);
    persistAttempt(notificationId, 3);

    List<WebhookNotificationAttempt> attempts =
        webhookNotificationAttemptRepository.findByNotificationId(notificationId).await().indefinitely();

    assertEquals(3, attempts.size());
    assertEquals(List.of(1, 2, 3), attempts.stream().map(WebhookNotificationAttempt::getAttemptNumber).toList());
    assertTrue(attempts.stream().allMatch(attempt -> notificationId.equals(attempt.getNotificationId())));
  }

  @Test
  void findByNotificationId_shouldReturnAnEmptyListWhenNoAttemptsExist() {
    List<WebhookNotificationAttempt> attempts =
        webhookNotificationAttemptRepository.findByNotificationId(new ObjectId()).await().indefinitely();

    assertTrue(attempts.isEmpty());
  }

  private void persistAttempt(ObjectId notificationId, int attemptNumber) {
    WebhookNotificationAttempt attempt = new WebhookNotificationAttempt();
    attempt.setId(new ObjectId());
    attempt.setNotificationId(notificationId);
    attempt.setWebhookId(new ObjectId());
    attempt.setAttemptNumber(attemptNumber);
    attempt.setOutcome(WebhookNotification.NotificationStatus.DELIVERED);
    attempt.setStartedAt(LocalDateTime.now().minusSeconds(1));
    attempt.setFinishedAt(LocalDateTime.now());
    webhookNotificationAttemptRepository.persist(attempt).await().indefinitely();
  }
}
