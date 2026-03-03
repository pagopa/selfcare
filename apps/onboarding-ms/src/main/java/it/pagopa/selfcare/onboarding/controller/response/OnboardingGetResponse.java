package it.pagopa.selfcare.onboarding.controller.response;

import lombok.Data;

import java.util.List;

@Data
public class OnboardingGetResponse {
    Long count;
    List<OnboardingGet> items;
}
