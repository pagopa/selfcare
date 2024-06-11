package it.pagopa.selfcare.user.model;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserToNotify {

    private String userId;
    private String name;
    private String familyName;
    private String email;
    private PartyRole role;
    private String productRole;
    private OnboardedProductState relationshipStatus;

}
