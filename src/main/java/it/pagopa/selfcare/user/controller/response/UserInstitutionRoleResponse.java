package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import lombok.Data;

@Data
public class UserInstitutionRoleResponse {

    private String institutionId;
    private String institutionName;
    private PartyRole role;

}
