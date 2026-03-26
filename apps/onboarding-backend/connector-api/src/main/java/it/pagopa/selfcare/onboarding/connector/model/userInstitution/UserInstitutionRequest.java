package it.pagopa.selfcare.onboarding.connector.model.userInstitution;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
