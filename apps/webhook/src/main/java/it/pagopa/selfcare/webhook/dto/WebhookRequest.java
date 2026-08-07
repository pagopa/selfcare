package it.pagopa.selfcare.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class WebhookRequest {

  @NotBlank(message = "URL is required")
  private String url;

  @NotBlank(message = "Tenant ID is required")
  private String tenantId;

  @NotBlank(message = "ProductId is required")
  private String productId;

  @NotNull(message = "HTTP method is required")
  private String httpMethod;

  private Map<String, String> headers;

  /**
   * Topics the consumer wants to receive notifications for (e.g. SC-Contracts, SC-User,
   * SC-Delegate). When null or empty, notifications for every topic are sent.
   */
  private List<String> topics;

  private RetryPolicyRequest retryPolicy;

  @Data
  public static class RetryPolicyRequest {
    private Integer maxAttempts;
    private Long initialDelayMs;
    private Long maxDelayMs;
    private Double backoffMultiplier;
  }
}
