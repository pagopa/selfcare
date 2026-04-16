package it.pagopa.selfcare.mscore.model.onboarding;

import it.pagopa.selfcare.mscore.model.aggregation.UserInstitutionBinding;
import it.pagopa.selfcare.mscore.model.institution.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnboardingInfo {
    private String userId;
    private Institution institution;
    private UserInstitutionBinding binding;
}
