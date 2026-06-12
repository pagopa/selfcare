package it.pagopa.selfcare.external_api.model.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnboardedProductResponse {
  private String productId;
  private String status;
  private String productRole;
  private String productRoleLabel;
  private String role;
  private LocalDateTime createdAt;

}
