package it.pagopa.selfcare.onboarding.connector.rest.model;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingsResponse {

    private List<OnboardingResponse> onboardings;
}
