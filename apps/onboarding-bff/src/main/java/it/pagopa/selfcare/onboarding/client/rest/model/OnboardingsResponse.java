package it.pagopa.selfcare.onboarding.client.rest.model;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingsResponse {

    private List<OnboardingResponse> onboardings;
}
