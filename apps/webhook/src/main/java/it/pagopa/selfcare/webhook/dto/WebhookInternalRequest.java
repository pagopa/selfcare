package it.pagopa.selfcare.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Data
public class WebhookInternalRequest extends WebhookRequest {

  @NotBlank(message = "ProductId is required")
  private String productId;

  private String description;

  private List<String> products;
}
