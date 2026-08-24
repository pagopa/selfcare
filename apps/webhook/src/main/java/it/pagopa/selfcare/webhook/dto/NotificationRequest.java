package it.pagopa.selfcare.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequest {

  @NotBlank(message = "Product ID is required")
  private String productId;

  @NotBlank(message = "Tenant ID is required")
  private String tenantId;

  @NotBlank(message = "Payload is required")
  private String payload;

  /**
   * Topic of the notification (e.g. SC-Contracts, SC-Users, SC-Delegate). Only webhooks whose
   * configured topics include this value (or that have no topic filter configured) will receive the
   * notification.
   */
  @NotBlank(message = "Topic is required")
  private String topic;
}
