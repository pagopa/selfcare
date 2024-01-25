package it.pagopa.selfcare.user.model.notification;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
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
