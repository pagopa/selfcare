package it.pagopa.selfcare.dashboard.model.user;

import lombok.Data;

import java.util.List;

/**
 * UserInstitutionWithActions
 */
@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor

public class UserInstitutionWithActionsDto {

  private String userId;

  private String institutionId;

  private String institutionDescription;

  private String institutionRootName;

  private List<OnboardedProductWithActions> products;

  private String userMailUuid;

}

