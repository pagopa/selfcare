package it.pagopa.selfcare.webhook.entity;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;

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
