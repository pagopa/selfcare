package it.pagopa.selfcare.user.event.entity;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.event.constant.OnboardedProductState;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(asEnum = true)
public class UserInstitutionRole {

    private String institutionId;
    private String institutionName;
    private String institutionRootName;
    private String userMailUuid;
    private PartyRole role;
    private OnboardedProductState state;

}
