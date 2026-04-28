package it.pagopa.selfcare.onboarding.client.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInstitutionRequest {
  private String institutionId;
  private List<String> productRoles;
  private List<String> products;
  private List<String> roles;
  private List<String> states;
  private String userId;
}
