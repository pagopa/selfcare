package it.pagopa.selfcare.dashboard.model.onboarding;

import lombok.Data;

import java.util.List;

@Data
public class OnBoardingInfo {
    private String userId;
    private List<OnboardingData> institutions;
}
