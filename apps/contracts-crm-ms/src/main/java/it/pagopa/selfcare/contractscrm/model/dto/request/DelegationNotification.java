package it.pagopa.selfcare.contractscrm.model.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Delegation event of the SC-Delegations topic (see delegation-cdc DelegationNotificationToSend). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegationNotification {

  public static final String TYPE_PSP_MEMBERSHIP = "PSP_MEMBERSHIP";
  public static final String TYPE_PSP_RECIPIENT = "PSP_RECIPIENT";
  public static final String STATUS_DELETED = "DELETED";

  private String id;
  private String delegationId;
  private String from;
  private String to;
  private String productId;
  private String type;
  private String status;
  private String createdAt;
  private String updatedAt;
  private String closedAt;
}
