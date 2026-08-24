package it.pagopa.selfcare.webhook.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.LocalDateTime;
import lombok.Data;
import org.bson.types.ObjectId;

/**
 * Immutable audit record of a single delivery attempt for a {@link WebhookNotification}. One
 * document is appended per attempt (initial send or retry), so the full history survives even
 * when the parent notification is reprocessed multiple times and its own status/attemptCount
 * fields get overwritten.
 */
@Data
@MongoEntity(collection = "webhookNotificationAttempts")
public class WebhookNotificationAttempt {

  private ObjectId id;
  private ObjectId notificationId;
  private ObjectId webhookId;
  private int attemptNumber;
  private WebhookNotification.NotificationStatus outcome;
  private Integer statusCode;
  private String errorMessage;
  private LocalDateTime startedAt;
  private LocalDateTime finishedAt;
}
