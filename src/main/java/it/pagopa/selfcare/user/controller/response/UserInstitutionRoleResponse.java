package it.pagopa.selfcare.user.controller.response;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import lombok.Data;

@Data
public class UserInstitutionRoleResponse {

    private String institutionId;
    private String institutionName;
    private String institutionRootName;
    private PartyRole role;
    private OnboardedProductState status;
}
