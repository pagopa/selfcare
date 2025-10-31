package it.pagopa.selfcare.webhook.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

@Data
@MongoEntity(collection = "webhook_notifications")
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
    
    public enum NotificationStatus {
        PENDING,
        SENDING,
        SUCCESS,
        FAILED,
        RETRY
    }
}
