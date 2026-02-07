package it.pagopa.selfcare.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NotificationRequest {

  @NotBlank(message = "Product ID is required")
  private String productId;

  @NotBlank(message = "Payload is required")
  private String payload;
}
