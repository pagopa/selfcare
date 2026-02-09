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
  private String productId;
  private String description;
  private String url;
  private String httpMethod;
  private Map<String, String> headers;
  private List<String> products;
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
