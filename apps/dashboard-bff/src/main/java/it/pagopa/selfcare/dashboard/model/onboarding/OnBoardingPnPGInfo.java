package it.pagopa.selfcare.dashboard.model.onboarding;

import lombok.Data;

import java.util.List;

@Data
public class OnBoardingPnPGInfo {
    private String userId;
    private List<OnboardingPnPGData> institutions;
}
