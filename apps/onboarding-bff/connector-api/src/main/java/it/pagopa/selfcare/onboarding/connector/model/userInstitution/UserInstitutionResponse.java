package it.pagopa.selfcare.onboarding.connector.model.userInstitution;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInstitutionResponse {

  @JsonProperty("id")
  private String id;

  @JsonProperty("userId")
  private String userId;

  @JsonProperty("institutionId")
  private String institutionId;

  @JsonProperty("institutionDescription")
  private String institutionDescription;

  @JsonProperty("institutionRootName")
  private String institutionRootName;

  @JsonProperty("userMailUuid")
  private String userMailUuid;
}
