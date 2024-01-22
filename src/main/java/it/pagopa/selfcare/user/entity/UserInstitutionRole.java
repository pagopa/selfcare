package it.pagopa.selfcare.user.entity;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
public class UserInstitutionRole {

    private String institutionId;
    private String institutionName;
    private PartyRole role;
    private OnboardedProductState status;

}
