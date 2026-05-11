package it.pagopa.selfcare.external_api.model.institution;

import it.pagopa.selfcare.external_api.model.onboarding.OnboardingResponseData;
import lombok.Data;

import java.util.List;

@Data
public class OnBoardingInfo {
    private String userId;
    private List<OnboardingResponseData> institutions;
}
