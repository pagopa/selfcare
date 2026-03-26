package it.pagopa.selfcare.onboarding.connector.rest.model;

import it.pagopa.selfcare.onboarding.connector.model.onboarding.OnboardingResponseData;
import lombok.Data;

import java.util.List;

@Data
public class OnBoardingInfo {
    private String userId;
    private List<OnboardingResponseData> institutions;
}
