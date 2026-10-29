package it.pagopa.selfcare.contractscrm.model.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Envelope of a notification delivered by webhook-ms. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegationWebhookRequest {

  private String tenantId;
  private String productId;
  private String topic;

  @NotNull @Valid private DelegationNotification payload;
}
