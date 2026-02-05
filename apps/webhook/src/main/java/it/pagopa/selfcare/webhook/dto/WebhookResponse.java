package it.pagopa.selfcare.webhook.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class WebhookResponse {

  //    private String id;
  private String productId;
  private String description;
  private String url;
  private String httpMethod;
  private Map<String, String> headers;
  private List<String> products;
  private String status;
  private RetryPolicyResponse retryPolicy;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @Data
  public static class RetryPolicyResponse {
    private Integer maxAttempts;
    private Long initialDelayMs;
    private Long maxDelayMs;
    private Double backoffMultiplier;
  }
}
