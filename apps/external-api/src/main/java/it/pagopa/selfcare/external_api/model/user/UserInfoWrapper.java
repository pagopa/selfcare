package it.pagopa.selfcare.external_api.model.user;

import it.pagopa.selfcare.external_api.model.onboarding.OnboardedInstitutionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoWrapper {

    private User user;
    private List<OnboardedInstitutionResponse> onboardedInstitutions;

}
