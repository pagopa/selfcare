package it.pagopa.selfcare.webhook.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.LocalDateTime;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
@MongoEntity(collection = "webhookNotifications")
public class WebhookNotification {

  private ObjectId id;
  private ObjectId webhookId;
  private String payload;
  private NotificationStatus status;
  private Integer attemptCount;
  private String lastError;
  private LocalDateTime createdAt;
  private LocalDateTime lastAttemptAt;
  private LocalDateTime completedAt;
  private Boolean processing = false;
  private LocalDateTime processingUntil;

  public enum NotificationStatus {
    PENDING,
    SENDING,
    SUCCESS,
    FAILED,
    RETRY
  }
}
