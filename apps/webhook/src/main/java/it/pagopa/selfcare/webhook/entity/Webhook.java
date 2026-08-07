package it.pagopa.selfcare.webhook.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
@MongoEntity(collection = "webhooks")
public class Webhook {

  private ObjectId id;
  private String tenantId;
  private String productId;
  private String description;
  private String url;
  private String httpMethod;
  private Map<String, String> headers;
  private List<String> products;
  /**
   * Topics the consumer wants to receive notifications for (e.g. SC-Contracts, SC-User,
   * SC-Delegate). When null or empty, the webhook receives notifications for every topic.
   */
  private List<String> topics;

  private WebhookStatus status;
  private RetryPolicy retryPolicy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String createdBy;

  public enum WebhookStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
  }
}
